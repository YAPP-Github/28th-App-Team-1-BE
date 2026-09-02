# Changelog

## [1.1.0](https://github.com/Team-Hilit/Hilit-BE/compare/v1.0.1...v1.1.0) (2026-09-02)


### ✨ Features

* 랜딩페이지 S3+CloudFront 호스팅 terraform 모듈 추가 ([c520344](https://github.com/Team-Hilit/Hilit-BE/commit/c520344a8c57896bb3a99eafa2bdd42ccadd5afa))
* 랜딩페이지 배포용 GitHub OIDC 역할 추가 ([e14187d](https://github.com/Team-Hilit/Hilit-BE/commit/e14187d2365070ec8fa3e3b6e324221b3133f38c))
* 랜딩페이지(S3+CloudFront) + team@hilit.my 수신메일(SES) 구성 ([dd0198b](https://github.com/Team-Hilit/Hilit-BE/commit/dd0198b89a48f0afd5738515d137a4fd1726b07d))
* 팀 수신메일 SES inbound → Lambda 포워딩 terraform 모듈 추가 ([f3142b1](https://github.com/Team-Hilit/Hilit-BE/commit/f3142b1672e6aeda3465ed52376ef99d98275549))


### 🔧 Chore

* Actuator + Micrometer 도입 (관리 포트 8081) ([470a67c](https://github.com/Team-Hilit/Hilit-BE/commit/470a67ced82b3f93b3b5e92363fa25b3e8280ffa)), closes [#173](https://github.com/Team-Hilit/Hilit-BE/issues/173)
* gitignore에 로컬 설정·email lambda zip 추가 ([7c7727e](https://github.com/Team-Hilit/Hilit-BE/commit/7c7727e0ce09f539c0365fd281f45d3d305110a4))


### 📝 Docs

* README 프로젝트 소개·실행 방법·팀 정보 작성 ([e5f9a1d](https://github.com/Team-Hilit/Hilit-BE/commit/e5f9a1df38e32a0ed2df69294c26768c40feaf9a))
* README 프로젝트 소개·실행 방법·팀 정보 작성 ([e807e36](https://github.com/Team-Hilit/Hilit-BE/commit/e807e3613488958a7458e81aea7e0337a1397ae2))

## [1.0.1](https://github.com/YAPP-Github/28th-App-Team-1-BE/compare/v1.0.0...v1.0.1) (2026-08-21)


### 🔧 Chore

* release-please PAT 배선으로 전환, 배포는 태그 트리거로 단순화 ([1fa55c5](https://github.com/YAPP-Github/28th-App-Team-1-BE/commit/1fa55c58796435f825cf9f4846dbb8ac7cc7c218))
* release-please 도입 (SemVer 태그·릴리즈 노트 자동화) ([007671b](https://github.com/YAPP-Github/28th-App-Team-1-BE/commit/007671b69687428f4a75e9d2d05b2ef71a1b0d2e))
* 배포 트리거를 main push 에서 릴리즈 태그(v*)로 전환 ([f44d9af](https://github.com/YAPP-Github/28th-App-Team-1-BE/commit/f44d9af37b0a64ad0dd3bae54571c349cf3cbd8d))


### 🤖 CI

* PR·main push 테스트 게이트 워크플로우 분리 ([084d4e2](https://github.com/YAPP-Github/28th-App-Team-1-BE/commit/084d4e296558422f68e57643b54a8cd51bdca8c3))
