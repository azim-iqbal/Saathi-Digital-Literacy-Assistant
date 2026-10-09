# Bounded single-host pilot. Persistent SQLite state and a same-host TLS proxy required.
# Not a stateless/multi-replica Cloud Run configuration.
FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 SAATHI_STATE_DIR=/var/lib/saathi
RUN groupadd --gid 10001 saathi && useradd --uid 10001 --gid saathi --no-create-home saathi
WORKDIR /opt/saathi
COPY deploy/requirements.txt deploy/requirements.txt
RUN pip install --no-cache-dir -r deploy/requirements.txt
COPY backend/*.py backend/
COPY deploy/gunicorn.conf.py deploy/gunicorn.conf.py
RUN mkdir -p /var/lib/saathi && chown saathi:saathi /var/lib/saathi && chmod 0700 /var/lib/saathi
USER 10001:10001
# Use Linux host networking with the same-host Caddy config. Port 8768 stays loopback-only.
CMD ["gunicorn", "--config", "deploy/gunicorn.conf.py", "backend.hosted:create_app()"]
