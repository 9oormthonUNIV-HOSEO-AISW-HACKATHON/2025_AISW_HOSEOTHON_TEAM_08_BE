# SedRoad Backend (Spring Boot)

세대공감 여행 서비스 백엔드 API 서버

## 사전 요구사항

- Java 17 이상
- Gradle (또는 Gradle Wrapper 사용)
- MySQL 데이터베이스

## 서버 접속

서버가 시작되면 다음 주소에서 접속할 수 있습니다:

- **API Base URL**: `http://localhost:3000/api`
- **Swagger UI**: `http://localhost:3000/swagger-ui.html`
- **API 문서**: `http://localhost:3000/v3/api-docs`

## 주요 API 엔드포인트

### 인증
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인

### 방 관리
- `POST /api/rooms/create` - 방 생성
- `POST /api/rooms/join` - 방 참여
- `GET /api/rooms/user/{userId}` - 사용자 방 목록
- `GET /api/rooms/{roomId}/participants` - 방 참여자 목록

### 여행 추천
- `POST /api/analyze` - 세대 차이 분석
- `POST /api/recommend` - 여행 추천 생성
- `GET /api/recommendations/personal/{userId}` - 개인 추천 조회
- `GET /api/recommendations/room/{roomId}` - 방 추천 조회
- `POST /api/trips/save` - 여행지 저장
- `GET /api/trips/saved/{userId}` - 저장된 여행지 조회

### 사용자 프로필
- `GET /api/users/{userId}/profile` - 사용자 프로필 조회

### 방 채팅
- `GET /api/rooms/{roomId}/comments` - 댓글 조회
- `POST /api/rooms/{roomId}/comments` - 댓글 작성
- `GET /api/rooms/{roomId}/votes` - 투표 조회
- `POST /api/rooms/{roomId}/votes` - 투표하기
