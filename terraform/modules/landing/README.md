# modules/landing

`www.hilit.my` 랜딩페이지(정적 사이트)를 **S3 + CloudFront** 로 호스팅한다.

- S3: 비공개 버킷. 정적 빌드 산출물(`dist/`)의 저장소.
- CloudFront: HTTPS 배포. OAC 로만 S3 에 접근한다(버킷은 퍼블릭 아님).
- ACM: `www` TLS 인증서(반드시 `us-east-1`). DNS 가 가비아라 검증 CNAME 은 **수동** 등록한다.

랜딩페이지 소스/빌드는 별도 레포(`Team-Hilit/landing-page`)에 있다. 이 모듈은 인프라만 만든다.

## 비용

CloudFront 영구 무료 티어(월 1TB 전송 + 1천만 요청) 범위 안이라 실질 **$0~$1/월**. S3 저장(수 MB)·요청은 무시할 수준.

## 적용 순서 (DNS 가 가비아라 2단계)

CloudFront 는 **ISSUED 된 인증서**만 참조할 수 있는데, 인증서 검증은 가비아에 CNAME 을 수동으로 넣어야 완료된다. 따라서 인증서를 먼저 만들고 → 가비아 등록 → 검증 완료 후 나머지를 만든다.

```bash
cd terraform/envs/dev

# 1) ACM 인증서만 먼저 생성
terraform apply -target=module.landing.aws_acm_certificate.this

# 2) 가비아에 넣을 검증 CNAME 확인
terraform output landing_acm_validation_records
#  → { name = "_xxx.www.hilit.my.", type = "CNAME", value = "_yyy.acm-validations.aws." }
#    가비아 DNS 관리에 이 CNAME 레코드를 추가한다. (host 앞부분은 도메인 제외하고 입력)

# 3) 검증 완료(보통 수 분~수십 분) 후 나머지 전체 생성
terraform apply
#    aws_acm_certificate_validation 이 ISSUED 될 때까지 대기 후 S3/CloudFront 생성

# 4) CloudFront 도메인 확인 → 가비아에 www CNAME 등록
terraform output landing_cloudfront_domain_name
#  → dxxxxxxxxx.cloudfront.net
#    가비아: www  CNAME  dxxxxxxxxx.cloudfront.net
```

## 배포(정적 산출물 업로드)

`landing-page` 레포에서 빌드 후 S3 에 sync 하고 CloudFront 캐시를 무효화한다.

```bash
BUCKET=$(terraform output -raw landing_bucket_name)
DIST=$(terraform output -raw landing_cloudfront_distribution_id)

# landing-page 레포에서:
npm ci && npm run build
aws s3 sync dist/ "s3://$BUCKET/" --delete --profile d14
aws cloudfront create-invalidation --distribution-id "$DIST" --paths '/*' --profile d14
```

CI 자동화(main push 시 위 과정 실행)는 `landing-page` 레포의 GitHub Actions 로 구성한다.
