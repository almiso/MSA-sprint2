#!/bin/bash
# Останавливаем всё и удаляем старые контейнеры
docker-compose down --remove-orphans

# Собираем свежий JAR-файл из твоего исправленного кода
# Это критически важно, чтобы изменения в BookingController попали в билд
./gradlew clean build

# Запускаем всё заново, принудительно пересобирая образ
docker-compose up -d --build