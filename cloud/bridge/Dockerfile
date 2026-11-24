FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY bridge.py .
USER 10001:10001
ENV PYTHONUNBUFFERED=1
CMD ["python", "bridge.py"]
