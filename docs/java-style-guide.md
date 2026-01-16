# Java Style Guide

Ce document définit les conventions de style et bonnes pratiques à respecter dans ce projet Java.

## 1. Formatage du code
- Indentation de 4 espaces (pas de tabulations).
- Longueur maximale des lignes : 120 caractères.
- Chaque fichier doit contenir une seule classe publique.
- Les accolades `{}` doivent toujours être utilisées, même pour les blocs à une ligne.

```java
if (condition) {
    doSomething();
}

for (int i = 0; i < 10; i++) {
    doSomething();
}
```

## 2. Nommage
- Classes : PascalCase (ex. `MyClass`).
- Méthodes et variables : camelCase (ex. `myMethod`, `myVariable
- Constantes : UPPER_SNAKE_CASE (ex. `MY_CONSTANT`).
- Packages : tout en minuscules, avec des points pour séparer les niveaux (ex. `com.example.project`).
- Interfaces : PascalCase, souvent avec un préfixe "I" (ex. `IMyInterface`).
- Évitez les abréviations ambiguës.
- Utilisez des noms significatifs et descriptifs.
- Préférez les noms complets aux abréviations.
- Utilisez des verbes pour les noms de méthodes (ex. `calculateTotal`).

## 3. Log
- Utilisez une bibliothèque de logging standard (ex. SLF4J avec Logback).
- Niveaux de log : TRACE, DEBUG, INFO, WARN, ERROR.
- Ne pas utiliser `System.out.println` ou `System.err.println` pour le logging.
- Inclure des messages clairs et informatifs dans les logs.
- Évitez de logger des informations sensibles (ex. mots de passe, données personnelles).*
- Dans un bloc `catch`, loggez toujours l'exception avec son stack trace.

```java
try {
    // code pouvant générer une exception
} catch (Exception e) {
    logger.log(Level.ERROR, "Une erreur est survenue lors de l'exécution de l'opération", e);
}
```
