# Граф сцены практики 05

Интерфейс собран программно, чтобы связь между окном, сценой, контейнерами и элементами управления была видна в Java-коде.

```text
Stage «Дневник питания КБЖУ»
└── Scene
    └── BorderPane root
        ├── top: VBox header
        │   ├── Label title
        │   └── Label subtitle
        ├── center: TableView<NutritionPlan>
        │   ├── TableColumn id
        │   ├── TableColumn name
        │   ├── TableColumn effectiveFrom
        │   └── TableColumn status
        └── right: VBox form
            ├── Label formTitle
            ├── Label nameLabel
            ├── TextField nameField
            ├── Button addButton
            └── Label messageLabel
```

## Назначение узлов

- `Stage` представляет главное окно и задаёт минимальный размер.
- `Scene` владеет одним корневым узлом.
- `BorderPane` распределяет заголовок, таблицу и форму без абсолютных координат.
- `VBox header` объединяет название приложения и назначение экрана.
- `TableView` показывает снимок планов питания текущего сеанса.
- `VBox form` группирует единственный сценарий ввода и сообщение о результате.
- `TextField` принимает название нового плана.
- `Button` запускает одну операцию добавления.
- `messageLabel` показывает подтверждение или способ исправить ошибку.

База данных в граф не входит. Список существует только в памяти процесса и после закрытия приложения создаётся заново.
