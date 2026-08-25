# 아래 값들은 DNS(가비아)에 수동 등록해야 SES 수신이 동작한다.

output "verification_txt" {
  description = "도메인 소유 검증용 TXT. 호스트: _amazonses.<도메인>, 값: 아래 토큰"
  value = {
    host  = "_amazonses.${var.mail_domain}"
    type  = "TXT"
    value = aws_ses_domain_identity.this.verification_token
  }
}

output "dkim_cname_records" {
  description = "DKIM 서명용 CNAME 3개. 호스트: <token>._domainkey.<도메인>, 값: <token>.dkim.amazonses.com"
  value = [
    for token in aws_ses_domain_dkim.this.dkim_tokens : {
      host  = "${token}._domainkey.${var.mail_domain}"
      type  = "CNAME"
      value = "${token}.dkim.amazonses.com"
    }
  ]
}

output "mx_record" {
  description = "수신용 MX 레코드. 호스트: <도메인>(apex), 우선순위 10"
  value = {
    host     = var.mail_domain
    type     = "MX"
    priority = 10
    value    = "inbound-smtp.${data.aws_region.current.name}.amazonaws.com"
  }
}

output "spf_txt_record" {
  description = "포워딩 메일 도착률용 SPF TXT (선택). 호스트: <도메인>(apex)"
  value = {
    host  = var.mail_domain
    type  = "TXT"
    value = "v=spf1 include:amazonses.com ~all"
  }
}

output "inbox_bucket" {
  description = "수신 원문이 저장되는 S3 버킷"
  value       = aws_s3_bucket.inbox.id
}
