Grammar
=====

### Opus #25

Standard resources for building an object notation.

### Description

This library is a framework for making parsers for existing or new object notations.

### Motivation

I found that I was duplicating a lot of work
between [Argo](https://github.com/Moderocky/Argo), [Fern](https://github.com/Moderocky/Fern)
and [NBT](https://github.com/Moderocky/NBT),
particularly the conversion between objects and pseudo-primitive types.

Although each library had its own specific requirements it is usually not a good idea to duplicate work, so I have
attempted to collect the important parts of each into one resource.
To compensate for the individual requirements, internal parts of this library can be overridden where necessary.

## Maven Information

```xml

<repository>
    <id>kenzie</id>
    <name>Kenzie's Repository</name>
    <url>https://repo.kenzie.mx/releases</url>
</repository>
``` 

```xml

<dependency>
    <groupId>mx.kenzie</groupId>
    <artifactId>grammar</artifactId>
    <version>1.0.0</version>
</dependency>
```
