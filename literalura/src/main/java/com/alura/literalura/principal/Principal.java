package com.alura.literalura.principal;

import com.alura.literalura.models.*;
import com.alura.literalura.repository.IAutoresRepository;
import com.alura.literalura.repository.ILibrosRepository;
import com.alura.literalura.service.ConvierteDatos;
import com.alura.literalura.service.ConsumoApi;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private final Scanner scanner = new Scanner(System.in);
    private final ConsumoApi apiConsumer = new ConsumoApi();
    private final ConvierteDatos dataConverter = new ConvierteDatos();
    private static final String BASE_URL = "https://gutendex.com/books/?search=";

    private final IAutoresRepository autoresRepository;
    private final ILibrosRepository librosRepository;

    public Principal(IAutoresRepository autoresRepository, ILibrosRepository librosRepository) {
        this.autoresRepository = autoresRepository;
        this.librosRepository = librosRepository;
    }

    public void showMenu() {
        int option = -1;
        System.out.println("Bienvenido! Por favor selecciona una opción: ");
        while (option != 0) {
            String menu = """
                    1 - Buscar libros por título
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4 - Listar autores vivos en un determinado año
                    5 - Listar libros por idioma
                    6 - Top 10 libros más descargados
                    7 - Obtener estadísticas
                    0 - Salir
                    """;
            System.out.println(menu);
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> addBooks();
                case 2 -> listRegisteredBooks();
                case 3 -> listRegisteredAuthors();
                case 4 -> listAuthorsByYear();
                case 5 -> listBooksByLanguage();
                case 6 -> topTenBooks();
                case 7 -> showApiStatistics();
                case 0 -> System.out.println("Cerrando aplicación...");
                default -> System.out.println("Opción no válida, intenta de nuevo");
            }
        }
    }

    private Datos fetchBookData() {
        String bookTitle = scanner.nextLine();
        String json = apiConsumer.obtenerDatos(BASE_URL + bookTitle.replace(" ", "+"));
        return dataConverter.obtenerDatos(json, Datos.class);
    }

    private Libros createBook(DatosLibros bookData, Autores author) {
        if (author != null) {
            return new Libros(bookData, author);
        } else {
            System.out.println("El autor es null, no se puede crear el libro");
            return null;
        }
    }

    private void addBooks() {
        System.out.println("Escribe el libro que deseas buscar: ");
        Datos bookData = fetchBookData();
        if (!bookData.resultados().isEmpty()) {
            DatosLibros bookInfo = bookData.resultados().get(0);
            DatosAutores authorInfo = bookInfo.autor().get(0);
            Libros book = null;
            Libros existingBook = librosRepository.findByTitulo(bookInfo.titulo());
            if (existingBook != null) {
                System.out.println("Este libro ya se encuentra en la base de datos");
                System.out.println(existingBook);
            } else {
                Autores existingAuthor = autoresRepository.findByNameIgnoreCase(authorInfo.nombreAutor());
                if (existingAuthor != null) {
                    book = createBook(bookInfo, existingAuthor);
                    librosRepository.save(book);
                    System.out.println("----- LIBRO AGREGADO -----\n");
                    System.out.println(book);
                } else {
                    Autores author = new Autores(authorInfo);
                    author = autoresRepository.save(author);
                    book = createBook(bookInfo, author);
                    librosRepository.save(book);
                    System.out.println("----- LIBRO AGREGADO -----\n");
                    System.out.println(book);
                }
            }
        } else {
            System.out.println("El libro no existe en la API de Gutendex, ingresa otro");
        }
    }

    private void listRegisteredBooks() {
        List<Libros> books = librosRepository.findAll();
        if (books.isEmpty()) {
            System.out.println("No hay libros registrados");
            return;
        }
        System.out.println("----- LOS LIBROS REGISTRADOS SON: -----\n");
        books.stream()
                .sorted(Comparator.comparing(Libros::getTitulo))
                .forEach(System.out::println);
    }

    private void listRegisteredAuthors() {
        List<Autores> authors = autoresRepository.findAll();
        if (authors.isEmpty()) {
            System.out.println("No hay autores registrados");
            return;
        }
        System.out.println("----- LOS AUTORES REGISTRADOS SON: -----\n");
        authors.stream()
                .sorted(Comparator.comparing(Autores::getName))
                .forEach(System.out::println);
    }

    private void listAuthorsByYear() {
        System.out.println("Escribe el año en el que deseas buscar: ");
        int year = scanner.nextInt();
        scanner.nextLine();
        if (year < 0) {
            System.out.println("El año no puede ser negativo, intenta de nuevo");
            return;
        }
        List<Autores> authorsByYear = autoresRepository.findByAñoNacimientoLessThanEqualAndAñoMuerteGreaterThanEqual(year, year);
        if (authorsByYear.isEmpty()) {
            System.out.println("No hay autores registrados en ese año");
            return;
        }
        System.out.println("----- LOS AUTORES VIVOS REGISTRADOS EN EL AÑO " + year + " SON: -----\n");
        authorsByYear.stream()
                .sorted(Comparator.comparing(Autores::getName))
                .forEach(System.out::println);
    }

    private void listBooksByLanguage() {
        System.out.println("Escribe el idioma por el que deseas buscar: ");
        String languageMenu = """
                es - Español
                en - Inglés
                fr - Francés
                pt - Portugués
                """;
        System.out.println(languageMenu);
        String language = scanner.nextLine();
        if (!List.of("es", "en", "fr", "pt").contains(language)) {
            System.out.println("Idioma no válido, intenta de nuevo");
            return;
        }
        List<Libros> booksByLanguage = librosRepository.findByLenguajesContaining(language);
        if (booksByLanguage.isEmpty()) {
            System.out.println("No hay libros registrados en ese idioma");
            return;
        }
        System.out.println("----- LOS LIBROS REGISTRADOS EN EL IDIOMA SELECCIONADO SON: -----\n");
        booksByLanguage.stream()
                .sorted(Comparator.comparing(Libros::getTitulo))
                .forEach(System.out::println);
    }

    private void topTenBooks() {
        System.out.println("De donde quieres obtener los libros más descargados?");
        String sourceMenu = """
                1 - Gutendex
                2 - Base de datos
                """;
        System.out.println(sourceMenu);
        int option = scanner.nextInt();
        scanner.nextLine();

        if (option == 1) {
            System.out.println("----- LOS 10 LIBROS MÁS DESCARGADOS EN GUTENDEX SON: -----\n");
            String json = apiConsumer.obtenerDatos(BASE_URL);
            Datos data = dataConverter.obtenerDatos(json, Datos.class);
            List<Libros> books = new ArrayList<>();
            for (DatosLibros bookInfo : data.resultados()) {
                Autores author = new Autores(bookInfo.autor().get(0));
                Libros book = new Libros(bookInfo, author);
                books.add(book);
            }
            books.stream()
                    .sorted(Comparator.comparing(Libros::getNumeroDescargas).reversed())
                    .limit(10)
                    .forEach(System.out::println);
        } else if (option == 2) {
            System.out.println("----- LOS 10 LIBROS MÁS DESCARGADOS EN LA BASE DE DATOS SON: -----\n");
            List<Libros> books = librosRepository.findAll();
            if (books.isEmpty()) {
                System.out.println("No hay libros registrados");
                return;
            }
            books.stream()
                    .sorted(Comparator.comparing(Libros::getNumeroDescargas).reversed())
                    .limit(10)
                    .forEach(System.out::println);
        } else {
            System.out.println("Opción no válida, intenta de nuevo");
        }
    }

    private void showApiStatistics() {
        System.out.println("De donde quieres obtener las estadísticas: ");
        String sourceMenu = """
                1 - Gutendex
                2 - Base de datos
                """;
        System.out.println(sourceMenu);
        int option = scanner.nextInt();
        scanner.nextLine();

        if (option == 1) {
            System.out.println("----- ESTADÍSTICAS DE DESCARGAS EN GUTENDEX -----\n");
            String json = apiConsumer.obtenerDatos(BASE_URL);
            Datos data = dataConverter.obtenerDatos(json, Datos.class);
            DoubleSummaryStatistics stats = data.resultados().stream()
                    .collect(Collectors.summarizingDouble(DatosLibros::numeroDescargas));
            System.out.println("Libro con más descargas: " + stats.getMax());
            System.out.println("Libro con menos descargas: " + stats.getMin());
            System.out.println("Promedio de descargas: " + stats.getAverage());
            System.out.println();
        } else if (option == 2) {
            System.out.println("----- ESTADÍSTICAS DE DESCARGAS EN LA BASE DE DATOS -----\n");
            List<Libros> books = librosRepository.findAll();
            if (books.isEmpty()) {
                System.out.println("No hay libros registrados");
                return;
            }
            DoubleSummaryStatistics stats = books.stream()
                    .collect(Collectors.summarizingDouble(Libros::getNumeroDescargas));
            System.out.println("Libro con más descargas: " + stats.getMax());
            System.out.println("Libro con menos descargas: " + stats.getMin());
            System.out.println("Promedio de descargas: " + stats.getAverage());
            System.out.println();
        } else {
            System.out.println("Opción no válida, intenta de nuevo");
        }
    }
}
