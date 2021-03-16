package tv.lid.cinema.api5.controllers;

import java.util.List;

import io.jooby.Context;
import io.jooby.Route;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import tv.lid.cinema.api5.models.MovieModel;

// класс контроллера управления фильмами
public final class MovieController extends CommonController {
    private static final int MOVIES_PER_PAGE = 10; // количество записей на страницу

    // запрос списка фильмов
    @Operation(
        summary     = "Запрос списка фильмов",
        description = "Роут постранично выводит список фильмов с сортировкой по дате выхода",
        parameters  = {
            @Parameter(
                in = ParameterIn.PATH,
                name = "page",
                required = false,
                description = "Номер отображаемой страницы, начиная с 1. Необязательный параметр, если не указывать, то будет выведена первая страница.",
                schema = @Schema(
                    defaultValue = "1",
                    minimum      = "1",
                    allOf        = { Integer.class }
                ),
                style = ParameterStyle.SIMPLE
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос был успешно выполнен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Result list(final Context ctx) {
        // считываем номер страницы во входных параметрах
        int page = 1;

        try {
            page = Integer.parseInt(ctx.path("page").value());
        } catch (Exception exc) {}

        // запрашиваем количество записей и вычисляем число страниц
        int total, pages;
        try {
            total = MovieModel.count();
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось получить количество записей в таблице фильмов!");
        }
        pages = (int) Math.ceil(total / MovieController.MOVIES_PER_PAGE);

        // запрашиваем список записей в соответствии с номером страницы
        List<MovieModel> list;
        try {
            list = MovieModel.list(page, MovieController.MOVIES_PER_PAGE);
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось получить список фильмов по заданным параметрам!");
        }

        // возвращаем результат в обёртке списка
        return ok(new ListWrapper(
            list,
            total,
            pages
        ));
    }

    // создать новый фильм
    @Operation(
        summary     = "Создать новый фильм",
        description = "Роут создаёт новый фильм в базе данных по заданным параметрам"
    )
    @RequestBody(
        description = "Данные по фильму",
        required    = true,
        content     = @Content(schema = @Schema(implementation = MovieModel.class))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос был успешно выполнен"),
        @ApiResponse(responseCode = "400", description = "Заданы некорректные входные данные запроса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Result create(final Context ctx) {
        // преобразовываем входные данные в модель
        MovieModel movie = ctx.body(MovieModel.class);

        // проверка корректности полученных данных
        if (movie == null || movie.id != 0) {
            return error(Code.BAD_REQUEST, "Заданы некорректные входные данные запроса!");
        }

        // сохраняем фильм в БД
        try {
            movie.save();
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось сохранить информацию о фильме в базе данных!");
        }

        // сообщаем об успехе
        return ok();
    }

    // найти фильм по заданному идентификатору
    @Operation(
        summary     = "Найти фильм по заданному идентификатору",
        description = "Роут запрашивает в базе данных информацию о фильме по заданному идентификатору",
        parameters  = {
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                required = true,
                description = "Идентификатор запрашиваемого фильма",
                schema = @Schema(
                    minimum = "1",
                    allOf   = { Integer.class }
                ),
                style = ParameterStyle.SIMPLE
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос был успешно выполнен"),
        @ApiResponse(responseCode = "400", description = "Заданы некорректные входные данные запроса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Result find(final Context ctx) {
        MovieModel movie;
        int        id;

        // считываем идентификатор фильма во входных параметрах
        try {
            id = Integer.parseInt(ctx.path("id").value());

            // ищем фильм по заданному идентификатору
            movie = MovieModel.find(id);
            if (movie == null) {
                throw new Exception();
            }
        } catch (Exception exc) {
            return error(Code.BAD_REQUEST, "Задан некорректный идентификатор фильма!");
        }

        // возвращаем фильм
        return ok(movie);
    }

    // изменить ранее созданный фильм
    @Operation(
        summary     = "Изменить ранее созданный фильм",
        description = "Роут изменяет фильм в базе данных в соответствии с заданными параметрами"
    )
    @RequestBody(
        description = "Данные по фильму",
        required    = true,
        content     = @Content(schema = @Schema(implementation = MovieModel.class))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос был успешно выполнен"),
        @ApiResponse(responseCode = "400", description = "Заданы некорректные входные данные запроса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Result modify(final Context ctx) {
        MovieModel movie = ctx.body(MovieModel.class);

        // проверка корректности полученных данных
        try {
            if (movie == null || !MovieModel.exists(movie.id)) {
                throw new Exception();
            }
        } catch (Exception exc) {
            return error(Code.BAD_REQUEST, "Заданы некорректные входные данные запроса!");
        }

        // сохраняем фильм в БД
        try {
            movie.save();
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось сохранить информацию о фильме в базе данных!");
        }

        // сообщаем об успехе
        return ok();
    }

    // удалить фильм по заданному идентификатору
    @Operation(
        summary     = "Удалить фильм по заданному идентификатору",
        description = "Роут удаляет фильм из базы данных по заданному идентификатору",
        parameters  = {
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                required = true,
                description = "Идентификатор удаляемого фильма",
                schema = @Schema(
                    minimum = "1",
                    allOf   = { Integer.class }
                ),
                style = ParameterStyle.SIMPLE
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос был успешно выполнен"),
        @ApiResponse(responseCode = "400", description = "Заданы некорректные входные данные запроса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Result kill(final Context ctx) {
        int id;

        // считываем идентификатор фильма во входных параметрах
        try {
            id = Integer.parseInt(ctx.path("id").value());

            // проверяем существование фильма по заданному идентификатору
            if (!MovieModel.exists(id)) {
                throw new Exception();
            }
        } catch (Exception exc) {
            return error(Code.BAD_REQUEST, "Задан некорректный идентификатор фильма!");
        }

        // удаляем фильм из БД
        try {
            MovieModel.kill(id);
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось удалить информацию о фильме из базы данных!");
        }

        // сообщаем об успехе
        return ok();
    }
}
