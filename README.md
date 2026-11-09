# barofit-identity

**barofit** 백엔드의 인증/사용자 서비스 레포입니다. 원래 `backend_baro` 모노레포의
`services/identity` 모듈을 폴리레포 전환 과정에서 분리했습니다(커밋 이력 보존).

소셜 로그인(Google/Kakao) → JWT(RS256) 발급·검증, 온보딩/프로필 관리, 프로필 사진 저장을 담당합니다.
다른 서비스는 `.well-known/jwks.json`으로 공개키만 받아가며, 이 서비스와 직접 통신하지 않고도
JWT를 검증합니다.

## 기술 스택

- Java 21 / Gradle (단일 모듈, 독립 레포)
- Spring Boot 4.1 (Web MVC, Data JPA, OAuth2 Resource Server, Validation)
- PostgreSQL `identity` 스키마, Flyway
- AWS SDK S3 (프로필 사진, 선택적)

## 의존성

[barofit-common](https://github.com/Barofit-team/barofit-common)의 `barofit-runtime`을 참조합니다.
먼저 그 레포에서 `./gradlew publishToMavenLocal`을 실행해둬야 이 레포가 빌드됩니다.

## 로컬 실행

```bash
# 1. JWT 키 생성 (최초 1회, keys/는 git에 커밋 안 됨)
./scripts/generate-jwt-keys.sh

# 2. .env 준비 (DB 접속 정보, JWT_* 등)
cp .env.example .env   # 레포에 .env.example이 없다면 barofit-infra의 것을 참고

# 3. barofit-common publishToMavenLocal 먼저 실행(최신 상태 확인)

# 4. 실행
./gradlew bootRun   # 기본 포트 3004
```

| 설정 | 내용 |
|---|---|
| `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_JWKS_PATH` / `JWT_CURRENT_KID` | RS256 서명/검증 키. 저장소에 커밋 금지 |
| `GOOGLE_CLIENT_IDS` / `KAKAO_APP_KEYS` | 소셜 로그인 ID Token aud 허용 목록(쉼표 구분) |
| `PROFILE_IMAGE_STORAGE` | `temporary`(기본, 파일 미보관) 또는 `s3` |

## 테스트 / 빌드

```bash
./gradlew test    # Docker 필요(Testcontainers)
./gradlew build   # build/libs/app.jar
```

## Docker 이미지

```bash
DOCKERHUB_USER=<hub-user> ./scripts/build-and-push.sh
```
`barofit-runtime`이 아직 원격 Maven 저장소에 없는 동안은 `~/.m2/repository/com/barofit`을
빌드 컨텍스트로 임시 포함합니다(`.m2-cache/`, 스크립트가 자동 처리·정리).

## 배포

로컬 minikube 배포는 [barofit-infra](https://github.com/Barofit-team/barofit-infra)의
`infra/charts/identity` Helm 차트로 합니다. 이 레포엔 Helm 차트를 두지 않습니다.

## API 요약

| API | 인증 | 설명 |
|---|---|---|
| `POST /api/v1/auth/social-login` | 없음 | `{provider, idToken}` → 토큰 발급(신규면 가입) |
| `POST /api/v1/auth/social-login/code` | 없음 | 인가 코드 → idToken 교환 후 동일 응답 |
| `POST /api/v1/auth/refresh` | 없음 | refresh token 회전 |
| `POST /api/v1/auth/logout` | 없음 | 세션 폐기 |
| `GET/PATCH /api/v1/users/me` | Access Token | 내 정보 조회/수정 |
| `POST /api/v1/users/me/onboarding` | Access Token | 온보딩 저장 |
| `PUT /api/v1/users/me/profile-image` | Access Token | 프로필 사진 변경 |
| `DELETE /api/v1/users/me` | Access Token | 탈퇴 |
| `GET /.well-known/jwks.json` | 없음 | JWT 공개키 |

## 관련 레포

- [barofit-common](https://github.com/Barofit-team/barofit-common) — 공유 라이브러리(이 레포가 의존)
- [barofit-routine](https://github.com/Barofit-team/barofit-routine) · [barofit-workout](https://github.com/Barofit-team/barofit-workout) · [barofit-analytics](https://github.com/Barofit-team/barofit-analytics) · [barofit-report](https://github.com/Barofit-team/barofit-report)
- [barofit-infra](https://github.com/Barofit-team/barofit-infra) — Helm 차트, `integration-tests`(이 서비스를 참조), 로컬 검증 스크립트
- [backend_baro](https://github.com/Barofit-team/backend_baro) — 분리 이전 모노레포(참고용)
