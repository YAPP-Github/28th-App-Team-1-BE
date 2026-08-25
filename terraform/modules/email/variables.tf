variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

# 수신 도메인 (apex). 예: hilit.my → team@hilit.my 로 메일 수신
variable "mail_domain" {
  type = string
}

# 수신 대상 로컬파트 목록. 예: ["team@hilit.my"]
# 이 주소들로 온 메일만 규칙이 처리한다.
variable "recipients" {
  type = list(string)
}

# 포워딩 대상 주소 (기존에 쓰던 Gmail 등). tfvars 에서 채운다.
variable "forward_to" {
  type = string
}

# S3 에 저장된 수신 원문 보관 일수 (이후 자동 삭제)
variable "retention_days" {
  type    = number
  default = 7
}
