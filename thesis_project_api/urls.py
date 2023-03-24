from django.urls import path
from .views import ConversionResponseView

urlpatterns = [
    path('convert/', view=ConversionResponseView.as_view()),
]