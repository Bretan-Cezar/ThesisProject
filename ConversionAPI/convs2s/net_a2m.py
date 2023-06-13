# Derived from original implementation: https://github.com/kamepong/ConvS2S-VC
# Papers: https://arxiv.org/abs/1811.01609 ; https://arxiv.org/abs/2104.06900

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
import time

import module as md

class EncoderAny(nn.Module):

    # 1D Dilated Non-Causal Convolution
    def __init__(self, in_ch, out_ch, mid_ch, num_layers=8, dor=0.1):
        super(EncoderAny, self).__init__()
        
        self.layer_names = []

        assert num_layers > 1

        self.num_layers = num_layers

        # RL ^ (512 <- 320) _ (1*1)
        self.start = md.DilConv1D(in_ch,mid_ch,1,1)

        # [1, 3, 9, 27, 1, 3, 9, 27]
        dilation = [3**(i%4) for i in range(num_layers)]

        self.glu_blocks = nn.ModuleList()

        for i in range(num_layers):

            # ResRGLU ^ (512 <- 512) _ (1*1)
            self.glu_blocks.append(md.DilConvGLU1D(mid_ch,mid_ch,5,dilation[i]))

        # RL ^ (1024 <- 512) _ (1*1)
        self.end = md.DilConv1D(mid_ch,out_ch*2,1,1)

        self.dropout = nn.Dropout(p=dor)
        

    def forward(self, x):
        
        out = self.dropout(x)

        # out: 320 -> 512
        out = self.start(out)

        for i, layer in enumerate(self.glu_blocks):

            # out: 512 -> 512
            out = layer(out)

        # out: 512 -> 1024
        out = self.end(out)

        # K: 512 ; V: 512
        K, V = torch.split(out, out.shape[1]//2, dim=1)

        return K, V
    

class ConvS2SAny2Many(nn.Module):

    def __init__(self, enc: EncoderAny, predec, postdec):
        super(ConvS2SAny2Many, self).__init__(enc, predec, postdec)

    def forward(self, in_s, in_t, c_t):
        K_s, V_s = self.enc(in_s)

        # K_s.shape: B x d x N
        d = K_s.shape[1]
        Q_t, _ = self.predec(in_t, c_t)
        # Q_t.shape: B x d x T

        # Attention matrix
        # Scaled dot-product attention
        A = F.softmax(torch.matmul(K_s.permute(0,2,1), Q_t)/np.sqrt(d), dim=1)

        # A.shape: B x N x T
        R = torch.matmul(V_s,A)
        # R.shape: B x d x T

        R = torch.cat((R,F.dropout(Q_t, p=0.9)), dim=1)

        y, _ = self.postdec(R, c_t)

        return y, A

    def calc_loss(self, x_s, x_t, m_s, m_t, c_t, pos_weight=1.0,
                          gauss_width_da=0.3, reduction_factor=3):
        # L1 loss with position encoding
        device = x_s.device
        rf = reduction_factor
        # x_s.shape: batchsize x num_mels x N
        # x_t.shape: batchsize x num_mels x T
        #N = x_s.shape[2]
        #T = x_t.shape[2]
        num_mels = x_s.shape[1]
        BatchSize = x_s.shape[0]

        x_s = self.subsample(x_s,rf)
        x_t = self.subsample(x_t,rf)

        # Pad all-zero frame
        x_t = self.pad_zero_frame(x_t)
        
        B,D,N = x_s.shape
        B,D,T = x_t.shape
        assert D == num_mels*rf
        
        pos_s = md.position_encoding(N, D)
        pos_t = md.position_encoding(T, D)

        pos_s = torch.tensor(pos_s).to(device, dtype=torch.float)
        pos_t = torch.tensor(pos_t).to(device, dtype=torch.float)

        pos_s = pos_s.repeat(BatchSize,1,1)
        pos_t = pos_t.repeat(BatchSize,1,1)

        scale_emb = D**0.5

        in_s = x_s
        in_s[:,0:pos_s.shape[1],:] = in_s[:,0:pos_s.shape[1],:] + pos_s/scale_emb * pos_weight 

        in_t = x_t
        in_t[:,0:pos_t.shape[1],:] = in_t[:,0:pos_t.shape[1],:] + pos_t/scale_emb * pos_weight
        
        m_s = m_s[:,:,0::rf]
        m_t = m_t[:,:,0::rf]

        zero = torch.tensor(np.zeros((BatchSize,1,1))).to(device, dtype=torch.float)

        m_t = torch.cat((zero,m_t),dim=2)

        assert m_s.shape[2] == N
        assert m_t.shape[2] == T
        
        y, A = self(in_s, in_t, c_t)

        # Main Loss
        MainLoss = torch.sum( torch.mean(m_t[:,:,1:T].repeat(1,num_mels*rf,1) * torch.abs(y[:,:,0:T-1] - x_t[:,:,1:T]), 1) )

        MainLoss = MainLoss/torch.sum(m_t[:,:,1:T])

        W = np.zeros((BatchSize,N,T))

        # Compute Penalty Matrix
        for b in range(0,BatchSize):

            Nb = int(torch.sum(m_s[b,:,:]))
            Tb = int(torch.sum(m_t[b,:,:]))

            nN = np.arange(0,N)/Nb
            tT = np.arange(0,T)/Tb

            nN_tiled = np.tile(nN[:,np.newaxis], (1,T))
            tT_tiled = np.tile(tT[np.newaxis,:], (N,1))

            W[b,:,:] = 1.0-np.exp(-np.square(nN_tiled - tT_tiled)/(2.0*gauss_width_da**2))
            W[b,Nb:N,Tb:T] = 0.

        W = torch.tensor(W).to(device, dtype=torch.float)
        
        # Diagonal Attention Loss
        DALoss = torch.sum(torch.mean(A*W, 1))/torch.sum(m_t)

        A_np = A.detach().cpu().clone().numpy()
        
        return MainLoss, DALoss, A_np

    
    def inference(self, x_s, c_t, rf, pos_weight=1.0, attention_mode='raw'):
        start = time.time()
        
        device = x_s.device
        # x_s.shape: batchsize x num_mels x N
        num_mels = x_s.shape[1]

        x_s = self.subsample(x_s, rf)
        BatchSize,D,N = x_s.shape

        pos_s = md.position_encoding(N, D)
        pos_s = torch.tensor(pos_s).to(device, dtype=torch.float)
        pos_s = pos_s.repeat(BatchSize,1,1)
        scale_emb = D**0.5

        in_s = x_s
        in_s[:,0:pos_s.shape[1],:] = in_s[:,0:pos_s.shape[1],:] + pos_s/scale_emb * pos_weight
        x_t = torch.tensor(np.zeros((1,D,1))).to(device, dtype=torch.float)

        self.enc.eval()
        self.predec.eval()
        self.postdec.eval()

        with torch.no_grad():
            K, V = self.enc(in_s)
        d = K.shape[1]
        
        if attention_mode == 'raw' or attention_mode == None:
            # Raw attention
            T = round(N*2.0)
            in_t = x_t

            pos_t = md.position_encoding(T, D)
            pos_t = torch.tensor(pos_t).to(device, dtype=torch.float)
            pos_t = pos_t.repeat(BatchSize,1,1)

            state_out_predec = None
            state_out_postdec = None

            for t in range(0,T):

                in_t[:,0:pos_t.shape[1],:] = in_t[:,0:pos_t.shape[1],:] + pos_t[:,:,t:t+1]/scale_emb * pos_weight
                
                with torch.no_grad():

                    Q, state_out_predec = self.predec(in_t, c_t, state_out_predec)

                    # Scaled dot-product attention
                    A = F.softmax(torch.matmul(K.permute(0,2,1), Q)/np.sqrt(d), dim=1)
                    R = torch.matmul(V,A)
                    R = torch.cat((R,F.dropout(Q, p=0.0, training=False)), dim=1)

                    y, state_out_postdec = self.postdec(R,c_t, state_out_postdec)

                    y_concat = y if t == 0 else torch.cat((y_concat,y), dim=2)
                    A_concat = A if t == 0 else torch.cat((A_concat,A), dim=2)

                    in_t = y

            elapsed_time = time.time() - start
            A_np = A_concat[0,:,:].detach().cpu().clone().numpy()**0.3
            path = self.mydtw_fromDistMat(1.0-A_np,w=100,p=0.1)

            end_of_frame = path[1][-1]
            #end_of_frame = min(path[1][-1]+20, T)
            #end_of_frame = T
                
        elif attention_mode == 'diagonal':
            # Exactly diagonal attention (no time-warping)
            T = N
            end_of_frame = T
            in_t = x_t

            pos_t = md.position_encoding(T, D)
            pos_t = torch.tensor(pos_t).to(device, dtype=torch.float)
            pos_t = pos_t.repeat(BatchSize,1,1)

            state_out_predec = None
            state_out_postdec = None
            for t in range(0,T):

                in_t[:,0:pos_t.shape[1],:] = in_t[:,0:pos_t.shape[1],:] + pos_t[:,:,t:t+1]/scale_emb * pos_weight

                with torch.no_grad():
                    Q, state_out_predec = self.predec(in_t, c_t, state_out_predec)
                    R = torch.cat((V[:,:,t:t+1],F.dropout(Q, p=0.0, training=False)), dim=1)
                    y, state_out_postdec = self.postdec(R,c_t, state_out_postdec)
                    y_concat = y if t == 0 else torch.cat((y_concat,y), dim=2)
                    in_t = y

            elapsed_time = time.time() - start
            A_concat = np.eye(N).reshape(1,N,N)
            A_concat = torch.tensor(A_concat).to(device, dtype=torch.float)
            path = [np.arange(N), np.arange(N)]

        elif attention_mode == 'forward':
            # Forward attention
            T = round(N*2.0)
            n_argmax = 0
            y_samples = np.array([0])
            x_samples = np.array([0])
            in_t = x_t

            pos_t = md.position_encoding(T, D)
            pos_t = torch.tensor(pos_t).to(device, dtype=torch.float)
            pos_t = pos_t.repeat(BatchSize,1,1)

            state_out_predec = None
            state_out_postdec = None
            for t in range(0,T):

                in_t[:,0:pos_t.shape[1],:] = in_t[:,0:pos_t.shape[1],:] + pos_t[:,:,t:t+1]/scale_emb * pos_weight

                with torch.no_grad():
                    Q, state_out_predec = self.predec(in_t, c_t, state_out_predec)
                    # Scaled dot-product attention
                    A = F.softmax(torch.matmul(K.permute(0,2,1), Q)/np.sqrt(d), dim=1)

                    A_np = A.detach().cpu().clone().numpy()
                    # Prediction of attended time point
                    if t==0:
                        n_argmax_tmp = self.localpeak(A_np,n_argmax,5.0)[0]
                        if n_argmax_tmp > n_argmax:
                            n_argmax = n_argmax_tmp
                    else:
                        n_argmax_tmp = self.localpeak(A_np,n_argmax,5.0)[0]
                        y_samples = np.append(y_samples,n_argmax_tmp)
                        x_samples = np.append(x_samples,t)
                        slope = (np.mean((y_samples-np.mean(y_samples))*(x_samples-np.mean(x_samples)))
                                 /(max(np.std(x_samples),1e-10)**2))
                        n_argmax = int(round(slope*(t+1)))

                    A_np[0,0:max(n_argmax-20//rf,0),0] = 0
                    A_np[0,min(n_argmax+40//rf,N-1):,0] = 0
                    A_np = (np.maximum(A_np,1e-10))/np.sum(np.maximum(A_np,1e-10))
                    A_ = torch.tensor(A_np).to(device, dtype=torch.float)
                    A_concat = A_ if t == 0 else torch.cat((A_concat, A_), dim=2)
                    R = torch.matmul(V,A_)
                    R = torch.cat((R,F.dropout(Q, p=0.0, training=False)), dim=1)
                    y, state_out_postdec = self.postdec(R,c_t,state_out_postdec)
                    y_concat = y if t == 0 else torch.cat((y_concat,y), dim=2)

                    in_t = y

            elapsed_time = time.time() - start
            A_tmp = A_concat[0,:,:].detach().cpu().clone().numpy()**0.3
            #import pdb;pdb.set_trace() # Breakpoint
            path = self.mydtw_fromDistMat(1.0-A_tmp,w=100,p=0.1)
            end_of_frame = path[1][-1]
            #end_of_frame = T
            
        A_out = A_concat[:,:,0:end_of_frame].clone()
        A_out = A_out.detach().cpu().clone().numpy()

        melspec_conv = self.expand(y_concat[:,0:D,0:end_of_frame],rf).detach().cpu().clone().numpy()
        melspec_conv = melspec_conv[0,:,:]

        return melspec_conv, A_out, elapsed_time
