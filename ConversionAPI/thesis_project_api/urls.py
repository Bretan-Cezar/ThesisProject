from django.urls import path
from .views import ConversionResponseView

view = ConversionResponseView.as_view()

urlpatterns = [
    path('convert/', view=view),
]