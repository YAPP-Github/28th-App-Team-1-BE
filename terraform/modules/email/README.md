# email 모듈 — 팀 수신 메일 (SES inbound → Lambda 포워딩)

`team@hilit.my` 로 온 메일을 받아 지정한 Gmail 등으로 자동 전달한다.

## 구성

```
발신자 → MX(hilit.my) → SES 수신(us-east-1)
          └ 규칙: S3(inbox/<id> 저장) → Lambda 호출
                                          └ 원문 읽어 From rewrite 후 forward_to 로 재전송
```

- **리전**: 서울(ap-northeast-2)은 SES 수신 미지원. 호출측(`envs/dev/main.tf`)에서
  `aws.us_east_1` provider 를 주입해 us-east-1 에 생성한다.
- **비용**: SES 수신 월 1,000통 무료, S3/Lambda 프리티어 → 팀 수신용은 사실상 $0.
- **From rewrite**: 원 From 을 그대로 두면 SPF/DMARC 로 반송되므로, From 은
  `<원발신자이름> via hilit.my <team@hilit.my>` 로 바꾸고 원발신자는 Reply-To 로 보존한다.
  → 전달받은 메일에서 그냥 "답장"하면 원발신자에게 간다.

## 배포 순서

1. `terraform.tfvars` 의 `mail_forward_to` 를 실제 수신함 주소로 설정.
2. `terraform apply` — SES/S3/Lambda/규칙 생성.
3. `terraform output` 으로 나온 아래 레코드를 **가비아 DNS 에 수동 등록**:
   - `mail_verification_txt` — 도메인 소유 검증 TXT
   - `mail_dkim_cname_records` — DKIM CNAME 3개
   - `mail_mx_record` — 수신 MX (apex `hilit.my`)
   - `mail_spf_txt_record` — 포워딩 도착률용 SPF TXT (선택이지만 권장)
4. **forward_to 주소 확인**: `aws_ses_email_identity` 로 verify 메일이 그 주소로 발송된다.
   메일의 확인 링크를 클릭해야 전달이 동작한다(SES 샌드박스 제약).
5. 도메인이 verified 되면(수 분~수십 분) `team@hilit.my` 로 테스트 메일 발송 → forward_to 로 도착 확인.

## 참고 / 한계

- **샌드박스**: 신규 계정은 SES 샌드박스라 "verify 된 주소로만" 발송 가능. 포워딩 대상이
  1개면 위 4번으로 충분. 나중에 여러 곳으로 보내거나 제약 없이 쓰려면 SES production
  access 요청(무료, 콘솔에서 신청).
- **수신 원문**은 S3 `inbox/` 에 `retention_days`(기본 7일) 후 자동 삭제된다.
- 수신 주소를 늘리려면 `mail_recipients` 에 추가한다. From rewrite 의 발신 주소는
  목록의 첫 번째(`recipients[0]`)를 쓴다.
