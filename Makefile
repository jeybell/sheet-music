.DEFAULT_GOAL := help

help:
	@echo ""
	@echo "악보 정리 앱 (Sheet Music)"
	@echo ""
	@echo "사용 가능한 명령어:"
	@echo ""
	@echo "  개발"
	@echo "    make dev          백엔드 개발 서버 실행 (Spring Boot)"
	@echo "    make frontend     프론트엔드 개발 서버 실행 (Vue)"
	@echo ""
	@echo "  빌드"
	@echo "    make build        백엔드 빌드"
	@echo "    make build-all    백엔드 + 프론트엔드 빌드"
	@echo ""
	@echo "  Docker"
	@echo "    make up           전체 스택 실행 (docker-compose)"
	@echo "    make down         전체 스택 종료"
	@echo "    make logs         컨테이너 로그 출력"
	@echo "    make db           DB 컨테이너만 실행"
	@echo ""

dev:
	./gradlew bootRun

frontend:
	cd frontend && npm run dev

build:
	./gradlew build

build-all:
	./gradlew build
	cd frontend && npm run build

up:
	docker-compose up -d

down:
	docker-compose down

logs:
	docker-compose logs -f

db:
	docker-compose up -d postgres

.PHONY: help dev frontend build build-all up down logs db
