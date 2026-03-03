#!/bin/bash
# ================================================================
# setup-ec2.sh — Configura una instancia EC2 Ubuntu nueva
# Ejecutar como: sudo bash setup-ec2.sh
# ================================================================
set -euo pipefail

echo "══════════════════════════════════════════════"
echo "  Configurando EC2 para Parqueaderos API"
echo "══════════════════════════════════════════════"

# 1. Actualizar paquetes
echo "→ Actualizando paquetes..."
apt-get update -y && apt-get upgrade -y

# 2. Instalar Docker
echo "→ Instalando Docker..."
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 3. Agregar usuario ubuntu al grupo docker (para no necesitar sudo)
usermod -aG docker ubuntu

# 4. Habilitar Docker al inicio
systemctl enable docker
systemctl start docker

# 5. Instalar git
apt-get install -y git

# 6. Crear directorio de la app
echo "→ Creando directorio de la app..."
mkdir -p /home/ubuntu/parqueaderos-api
chown ubuntu:ubuntu /home/ubuntu/parqueaderos-api

echo ""
echo "══════════════════════════════════════════════"
echo "  ✅ EC2 lista!"
echo ""
echo "  Próximos pasos (como usuario ubuntu):"
echo "  1. exit  (salir de sudo)"
echo "  2. cd ~/parqueaderos-api"
echo "  3. git clone https://github.com/jbeleno/parqueadero-backend.git ."
echo "  4. cp .env.example .env   (editar si es necesario)"
echo "  5. docker compose up -d --build"
echo "  6. docker compose logs -f api   (ver logs)"
echo "══════════════════════════════════════════════"
