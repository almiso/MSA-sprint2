# Отчет о выполнении Задания 5: Управление трафиком с Istio

## Описание изменений и решений

В рамках Задания 5 в K8s Minikube был установлен Service Mesh Istio, а также настроена сложная маршрутизация трафика (Traffic Management) для `booking-service` в двух версиях (`v1` и `v2`).

### 1. Подготовка Helm-чарта под Istio (Versions)
Оригинальный Helm-чарт из Задания 4 был доработан так, чтобы поддерживать одновременный деплой нескольких версий одного приложения, где каждая версия имеет уникальный `Deployment`, но все они делят общий `Service`.
- В шаблоны Deployment были добавлены лейблы `version: {{ .Values.version }}` и изменены имена релизов для изоляции (`booking-service-v1` и `booking-service-v2`).
- Были созданы два Values-файла: `values-v1.yaml` (основная версия без Service Mesh фич) и `values-v2.yaml` (версия со включенным фича-флагом, сервис шарится с v1).

### 2. Установка Istio Profiles
- Установлен Istio через `istioctl install --set profile=demo`.
- Выполнена команда `kubectl label namespace default istio-injection=enabled` для автоматической инъекции Sidecar-прокси (Envoy) при создании всех новых подов в `default` пространстве.

### 3. Настройка Istio (CRDs)

Были написаны и применены 3 ключевых конфигурационных файла:

1. **`destination-rule.yaml` (Circuit Breaking):**
   - Настроен `DestinationRule` с двумя Subsets (`v1` и `v2`) по лейблу `version`.
   - Включен `outlierDetection` (Circuit Breaker): если под возвращает хотя бы 1 ошибку `5xx`, он исключается из пула балансировки на 3 минуты (`baseEjectionTime: 3m`).

2. **`virtual-service.yaml` (Canary & Fallback & Retries):**
   - Был создан `Gateway`, слушающий порт 80 для `istio-ingressgateway`.
   - Настроено канареечное распределение трафика (`weight`): 90% обращений идет на Subset `v1`, а 10% — осторожно направляется на `v2`.
   - Для реализации защиты от сбоев (`Fallback`) в `VirtualService` добавлены `retries` (Retry механизм): 3 попытки при ошибках `5xx` или сбоях коннекта.
   - *Примечание:* Если `v1` отказывает полностью (все поды `v1` выключены), Envoy возвращает 503 `no healthy upstream` и/или производит retry внутри доступного пула.

3. **`envoy-filter.yaml` (Feature Flag):**
   - Написан низкоуровневый фильтр `EnvoyFilter`, воздействующий на конфигурацию HTTP_ROUTE в Ingress Gateway.
   - Фильтр перехватывает все запросы, и если находит HTTP-заголовок `X-Feature-Enabled: true`, принудительно направляет этот запрос напрямую в кластер `v2` (`outbound|80|v2|booking-service.default.svc.cluster.local`), в обход канареечных весов.

### 4. Проверка и логи

- Успешно отработали все проверочные скрипты (`check-istio.sh`, `check-canary.sh`, `check-feature-flag.sh`, `check-fallback.sh`).
- Во время стресс-теста канареечного Split'а `check-canary.sh`, логи `istio-ingressgateway` показали математическое подтверждение распределения (90% ушло в v1, 10% в v2).
- При отключении пода `v1` (тестирование Fallback через масштабирование в 0), Envoy корректно отработал сброс соединений, показав `no healthy upstream` или retry механизмы в зависимости от состояния Circuit Breaker.

Все артефакты (yaml) и логи утилит выгружены в эту папку.
