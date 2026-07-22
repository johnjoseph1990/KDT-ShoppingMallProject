# Git 기초 학습 정리

KDT 쇼핑몰 프로젝트를 진행하면서 실제로 사용한 Git 명령어와 개념을 정리합니다.

---

## 1. Git이란?

**코드의 변경 이력을 저장하고, 여러 컴퓨터·여러 사람이 같은 코드를 함께 작업할 수 있게 해주는 버전 관리 도구입니다.**

- 내 컴퓨터 = **로컬 저장소(Local)**
- GitHub = **원격 저장소(Remote)**
- 둘 사이에서 코드를 주고받는 것이 Git의 핵심 역할

```
내 컴퓨터 (로컬)          GitHub (원격)
    ↑↓                        ↑↓
  작업·저장           ←→    공유·백업
```

---

## 2. 핵심 개념

### 커밋 (Commit)
변경 내용을 **저장점**으로 기록하는 단위.  
"여기까지 작업했다"는 스냅샷을 찍는 행위.

```
커밋 A → 커밋 B → 커밋 C (현재)
           ↑
     언제든 이 시점으로 되돌아갈 수 있다
```

### 브랜치 (Branch)
독립적인 작업 흐름. 이 프로젝트는 `master` 브랜치 하나만 사용.

### 스테이징 영역 (Staging Area)
커밋 전에 "이 파일을 커밋에 포함할게요"라고 선택해두는 임시 공간.  
`git add`로 파일을 여기에 올린다.

```
작업 공간(Working Directory)
    → git add →  스테이징 영역
                    → git commit → 로컬 저장소
                                       → git push → GitHub
```

### origin
GitHub 저장소 주소의 별칭.  
`origin` = `https://github.com/johnjoseph1990/KDT-ShoppingMallProject.git`

---

## 3. 자주 쓰는 명령어

### 현재 상태 확인

```bash
git status
```
- 수정된 파일, 스테이징된 파일, 커밋 안 된 파일을 보여줌
- **작업 전후로 습관처럼 실행하는 명령어**

```bash
git log --oneline
```
- 커밋 이력을 한 줄씩 간결하게 보여줌

```bash
git diff
```
- 아직 스테이징 안 된 변경 내용을 줄 단위로 보여줌 (빨간색 = 삭제, 초록색 = 추가)

---

### 코드 받아오기

```bash
git pull origin master
```
- GitHub의 최신 코드를 내 컴퓨터로 내려받아 합치기
- **다른 PC에서 작업 시작할 때 반드시 먼저 실행**
- `git fetch`(다운로드) + `git merge`(합치기)를 한 번에 처리

---

### 코드 올리기

```bash
# 1단계: 파일 스테이징 (커밋에 포함할 파일 선택)
git add 파일명
git add .          # 현재 폴더 전체 변경사항 (주의: 민감정보 포함될 수 있음)

# 2단계: 커밋 (로컬에 저장점 기록)
git commit -m "feat: 새 기능 추가"

# 3단계: 푸시 (GitHub에 업로드)
git push origin master
```

---

### 저장소 복제 (처음 새 PC에서 시작할 때)

```bash
git clone https://github.com/johnjoseph1990/KDT-ShoppingMallProject.git
```
- GitHub에 있는 전체 프로젝트를 내 컴퓨터로 복사
- 이미 clone 한 PC라면 이후로는 `git pull`만 사용

---

## 4. 이 프로젝트의 실제 작업 흐름

```
[작업 시작]
git pull origin master        ← 반드시 먼저!

[코드 수정]
...파일 수정...

[저장]
git status                    ← 뭐가 바뀌었는지 확인
git add backend/src/...       ← 특정 파일만 선택
git commit -m "feat: 설명"    ← 로컬에 기록

[공유]
git push origin master        ← GitHub에 업로드

[다른 PC로 이동]
git pull origin master        ← 이어서 작업
```

---

## 5. 커밋 메시지 규칙 (이 프로젝트)

```
feat:     새 기능 추가
fix:      버그 수정
refactor: 코드 구조 개선 (기능 변화 없음)
docs:     문서 작성·수정
Day N:    N일차 작업 요약
```

**예시**
```
feat: 회원 탈퇴 API 구현
fix: 로그아웃 후 리다이렉트 경로 수정
docs: Git 기초 학습 문서 추가
```

---

## 6. 충돌(Conflict) 발생 시

두 사람(또는 두 PC)이 같은 파일의 같은 줄을 다르게 수정했을 때 발생.

```
<<<<<<< HEAD (내 코드)
String name = "홍길동";
=======
String name = "김철수";
>>>>>>> origin/master (상대방 코드)
```

**해결 방법**
1. 파일을 열어서 `<<<<`, `====`, `>>>>` 표시를 찾는다
2. 둘 중 맞는 코드를 남기고 표시 줄을 모두 삭제
3. `git add 파일명` → `git commit`

---

## 7. 자주 하는 실수

| 실수 | 결과 | 예방법 |
|------|------|--------|
| 작업 전 `git pull` 안 함 | push 시 충돌 발생 | 작업 시작 = pull 시작 |
| `git add .` 로 민감정보 커밋 | 비밀번호·키 GitHub 노출 | 파일명 지정해서 add |
| 커밋 메시지를 "수정" 한 글자로 씀 | 나중에 이력 파악 불가 | 무엇을/왜 바꿨는지 작성 |
| push 안 하고 자리 이동 | 다른 PC에서 최신 코드 없음 | 작업 종료 = push 종료 |

---

## 8. 유용한 명령어 모음

```bash
git log --oneline -10         # 최근 10개 커밋만 보기
git diff HEAD                 # 마지막 커밋과 현재 차이
git restore 파일명             # 수정 전으로 되돌리기 (주의: 복구 불가)
git stash                     # 현재 작업을 임시 저장 (커밋 없이)
git stash pop                 # 임시 저장한 작업 다시 꺼내기
```
