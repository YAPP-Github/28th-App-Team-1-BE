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

# ──────────────────────────────────────────────────────────────
# 7. CloudWatch Agent 설치·설정 (호스트 리소스 메트릭 수집)
#    - 네임스페이스 D14/Host, 60초 간격, 커스텀 5개 (무료 티어 10개 내)
#    - append_dimensions로 InstanceId 단일 dimension만 부여(hostname dimension 미발행)
#      → 메트릭 개수를 정확히 5개로 유지하고 Terraform 알람이 InstanceId만으로 매칭
#    - disk는 drop_device=true (Nitro 재부팅 시 device명 변동으로 알람이 INSUFFICIENT DATA 되는 것 방지)
# ──────────────────────────────────────────────────────────────
cd /tmp
curl -fsSL "https://amazoncloudwatch-agent.s3.amazonaws.com/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb" \
  -o amazon-cloudwatch-agent.deb
dpkg -i -E amazon-cloudwatch-agent.deb
rm -f amazon-cloudwatch-agent.deb

cat > /opt/aws/amazon-cloudwatch-agent/etc/cloudwatch-agent.json << 'CWAGENT'
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "root"
  },
  "metrics": {
    "namespace": "D14/Host",
    "append_dimensions": {
      "InstanceId": "$${aws:InstanceId}"
    },
    "metrics_collected": {
      "mem": {
        "measurement": ["mem_used_percent", "mem_available"]
      },
      "swap": {
        "measurement": ["swap_used_percent"]
      },
      "disk": {
        "measurement": ["disk_used_percent"],
        "resources": ["/"],
        "drop_device": true
      },
      "cpu": {
        "totalcpu": true,
        "measurement": ["cpu_usage_iowait"]
      }
    }
  }
}
CWAGENT

/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config -m ec2 -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/cloudwatch-agent.json

echo "user_data.sh 완료" > /var/log/user_data.log
