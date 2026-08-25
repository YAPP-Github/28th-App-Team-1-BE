variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

# 랜딩페이지 커스텀 도메인 (예: www.hilit.my)
variable "domain_name" {
  type = string
}

# S3 버킷 이름 (전역 유일). 정적 빌드 산출물(dist)이 sync 되는 대상.
variable "bucket_name" {
  type = string
}

# CloudFront 가격 등급. PriceClass_200 은 아시아 엣지를 포함(한국 사용자 지연 최소).
variable "price_class" {
  type    = string
  default = "PriceClass_200"
}

# 배포 워크플로우가 있는 GitHub 레포 (owner/repo). OIDC 역할 신뢰 대상.
variable "github_repo" {
  type    = string
  default = "Team-Hilit/landing-page"
}
