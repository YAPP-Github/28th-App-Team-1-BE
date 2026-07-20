variable "aws_region" {
  description = "AWS 리전"
  type        = string
}

variable "aws_profile" {
  description = "AWS CLI named profile"
  type        = string
}

variable "project_name" {
  description = "프로젝트 이름 (리소스 네이밍에 사용)"
  type        = string
}

variable "environment" {
  description = "환경 태그 (dev / prod)"
  type        = string
}

variable "ec2_key_pair_name" {
  description = "SSH 접속용 AWS Key Pair 이름"
  type        = string
}

variable "allowed_ssh_cidr" {
  description = "SSH 접속 허용 CIDR"
  type        = string
  default     = "0.0.0.0/0"
}

variable "instance_type" {
  description = "EC2 인스턴스 타입"
  type        = string
  default     = "t3.micro"
}

variable "ami_id" {
  description = <<-EOT
    EC2에 사용할 Ubuntu 22.04 LTS AMI ID. 특정 ID로 고정한다.
    이전에는 data "aws_ami" (most_recent = true) 로 매 plan마다 최신 AMI를 조회했는데,
    시간이 지나 더 최신 AMI가 나오면 plan마다 인스턴스 교체(destroy+recreate)가 잡혔다.
    root volume이 delete_on_termination = true 라 교체 시 그 위 docker volume(postgres/redis 데이터)이
    함께 삭제되므로 위험하다. AMI를 의도적으로 올리고 싶을 때만 아래 명령으로 최신 ID를 조회해
    이 변수 값을 갱신한다:
      aws ec2 describe-images --owners 099720109477 \
        --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
                  "Name=virtualization-type,Values=hvm" \
        --query 'sort_by(Images, &CreationDate)[-1].ImageId' --output text \
        --region ap-northeast-2
  EOT
  type        = string
  default     = "ami-0afe1fd15675c3f15"
}

variable "spot_max_price" {
  description = "Spot 가격 상한 (USD/hr)"
  type        = string
  default     = "0.008"
}

variable "root_volume_size" {
  description = "루트 EBS 볼륨 크기 (GB)"
  type        = number
  default     = 20
}

variable "s3_bucket_suffix" {
  description = "S3 버킷 이름 suffix"
  type        = string
  default     = "storage"
}

variable "s3_cors_allowed_origins" {
  description = "S3 CORS 허용 origin 목록"
  type        = list(string)
  default     = ["*"]
}

variable "log_retention_days" {
  description = "CloudWatch 앱 로그그룹 보관 기간 (일)"
  type        = number
  default     = 14
}
