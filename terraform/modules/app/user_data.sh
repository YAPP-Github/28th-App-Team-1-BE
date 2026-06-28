#!/bin/bash
set -euo pipefail

# ──────────────────────────────────────────────────────────────
# 1. Swap 파일 2GB 생성 (t3.micro 1GB RAM 보완)
# ──────────────────────────────────────────────────────────────
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# ──────────────────────────────────────────────────────────────
# 2. 시스템 패키지 업데이트
# ──────────────────────────────────────────────────────────────
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y ca-certificates curl gnupg unzip

# ──────────────────────────────────────────────────────────────
# 3. Docker 설치
# ──────────────────────────────────────────────────────────────
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable --now docker
usermod -aG docker ubuntu

# ──────────────────────────────────────────────────────────────
# 4. AWS CLI 설치 (ECR pull 인증용)
# ──────────────────────────────────────────────────────────────
cd /tmp
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip
unzip -q awscliv2.zip
./aws/install
rm -rf aws awscliv2.zip

# ──────────────────────────────────────────────────────────────
# 5. 앱 작업 디렉터리 준비
# ──────────────────────────────────────────────────────────────
mkdir -p /opt/${project_name}
chown ubuntu:ubuntu /opt/${project_name}

# ──────────────────────────────────────────────────────────────
# 6. ECR 자동 로그인 헬퍼 스크립트 (배포 시 사용)
# ──────────────────────────────────────────────────────────────
cat > /usr/local/bin/ecr-login.sh << 'EOF'
#!/bin/bash
set -euo pipefail
REGION="${aws_region}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
EOF
chmod +x /usr/local/bin/ecr-login.sh

echo "user_data.sh 완료" > /var/log/user_data.log
