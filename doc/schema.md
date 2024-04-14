Database Schema
===============

# Database Information

```mermaid
erDiagram
    db_info {
        TEXT    name        "Application. `Easy Health Record`"
        INTEGER version     "Database verison. Current is `2`"
        TEXT    descript    "UTF-8. Health record description"
        INTEGER last_modify "The database last updated time in Unix tick (in seconds)"
    }
```

# Body Weight

Descriptions:

- `id`: The unique data identifier. Use Unix tick in seconds because users will not add more than one data in one second.
- `wc`: Added in version 2. Waistline.

```mermaid
erDiagram
    db_body_weight {
        INTEGER id          "Unique. Record ID"
        INTEGER date        "Record date time. Decimal that contains YYYY*10000000000 + MM*100000000 + DD*1000000 + hh*10000 + mm*100 + ss"
        REAL    weight      "Weight (kg,lb)"
        REAL    fat         "Body fat (%)"
        REAL    int_fat     "Visceral fat"
        REAL    bmi         "BMI"
        REAL    wc          "(version 2) Waistline (cm,inch)"
        REAL    bone        "Bone mass (kg,lb)"
        REAL    muscle      "Muscle mass (kg,lb)"
        REAL    water       "Body water (%)"
        INTEGER metabolic   "Basal metabolism (kcal)"
        INTEGER age         "Estimated physical age (years)"
        TEXT    comment     "Comment"
    }
```

# Blood Pressure

Descriptions:

- `id`: The unique data identifier. Use Unix tick in seconds because users will not add more than one data in one second.

```mermaid
erDiagram
    db_blood_pressure {
        INTEGER id          "Unique. Record ID"
        INTEGER date        "Record date time. Decimal that contains YYYY*10000000000 + MM*100000000 + DD*1000000 + hh*10000 + mm*100 + ss"
        INTEGER systolic    "Systolic (mmHg)"
        INTEGER diastolic   "Diastolic (mmHg)"
        INTEGER pulse       "Pulse (beats/minute)"
        TEXT    comment     "Comment"
    }
```

# Blood Glucose

Descriptions:

- `id`: The unique data identifier. Use Unix tick in seconds because users will not add more than one data in one second.

```mermaid
erDiagram
    db_blood_glucose {
        INTEGER id          "Unique. Record ID"
        INTEGER date        "Record date time. Decimal that contains YYYY*10000000000 + MM*100000000 + DD*1000000 + hh*10000 + mm*100 + ss"
        REAL    glucose     "Glucose (mg/dL,mmol/L)"
        INTEGER meal        "0: normal; 1: before meal (AC); 2: after meal (PC)"
        TEXT    comment     "Comment"
    }
```
