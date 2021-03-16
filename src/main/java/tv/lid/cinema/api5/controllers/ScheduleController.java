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
import tv.lid.cinema.api5.models.ScheduleModel;

// класс контроллера управления сеансами
public final class ScheduleController extends CommonController {
    private static final int SCHEDULES_PER_PAGE = 10; // количество записей на страницу

    // запрос списка сеансов
    @Operation(
        summary     = "Запрос списка сеансов",
        description = "Роут постранично выводит список сеансов заданного фильма с сортировкой по дате показа",
        parameters  = {
            @Parameter(
                in = ParameterIn.PATH,
                name = "movieId",
                required = true,
                description = "Идентификатор фильма, по которому запрашиваются сеансы",
                schema = @Schema(
                    minimum = "1",
                    allOf   = { Integer.class }
                ),
                style = ParameterStyle.SIMPLE
            ),
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
        @ApiResponse(responseCode = "400", description = "Заданы некорректные входные данные запроса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Result list (final Context ctx) {
        // считываем идентификатор фильма во входных параметрах
        int movieId;
        try {
            movieId = Integer.parseInt(ctx.path("movieId").value());
        } catch (Exception exc) {
            return error(Code.BAD_REQUEST, "Задан некорректный идентификатор фильма!");
        }

        // считываем номер страницы во входных параметрах
        int page = 1;

        try {
            page = Integer.parseInt(ctx.path("page").value());
        } catch (Exception exc) {}

        // запрашиваем количество записей и вычисляем число страниц
        int total, pages;
        try {
            total = ScheduleModel.count(movieId);
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось получить количество записей в таблице сеансов!");
        }
        pages = (int) Math.ceil(total / ScheduleController.SCHEDULES_PER_PAGE);

        // запрашиваем список записей в соответствии с номером страницы
        List<ScheduleModel> list;
        try {
            list = ScheduleModel.list(movieId, page, ScheduleController.SCHEDULES_PER_PAGE);
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось получить список сеансов по заданным параметрам!");
        }

        // возвращаем результат в обёртке списка
        return ok(new ListWrapper(
            list,
            total,
            pages
        ));
    }

    // создать новый сеанс
    @Operation(
        summary     = "Создать новый сеанс",
        description = "Роут создаёт новый сеанс в базе данных по заданным параметрам"
    )
    @RequestBody(
        description = "Данные по сеансу",
        required    = true,
        content     = @Content(schema = @Schema(implementation = ScheduleModel.class))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос был успешно выполнен"),
        @ApiResponse(responseCode = "400", description = "Заданы некорректные входные данные запроса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Result create(final Context ctx) {
        // преобразовываем входные данные в модель
        ScheduleModel schedule = ctx.body(ScheduleModel.class);

        // проверка корректности полученных данных
        try {
            if (schedule == null || !MovieModel.exists(schedule.movieId)) {
                throw new Exception();
            }
        } catch (Exception exc) {
            return error(Code.BAD_REQUEST, "Заданы некорректные входные данные запроса!");
        }

        // сохраняем фильм в БД
        try {
            schedule.save();
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось сохранить информацию о сеансе в базе данных!");
        }

        // сообщаем об успехе
        return ok();
    }

    // найти сеанс по заданному идентификатору
    @Operation(
        summary     = "Найти сеанс по заданному идентификатору",
        description = "Роут запрашивает в базе данных информацию о сеансе по заданному идентификатору",
        parameters  = {
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                required = true,
                description = "Идентификатор запрашиваемого сеанса",
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
        ScheduleModel schedule;
        int           id;

        // считываем идентификатор сеанса во входных параметрах
        try {
            id = Integer.parseInt(ctx.path("id").value());

            // ищем сеанс по заданному идентификатору
            schedule = ScheduleModel.find(id);
            if (schedule == null) {
                throw new Exception();
            }
        } catch (Exception exc) {
            return error(Code.BAD_REQUEST, "Задан некорректный идентификатор сеанса!");
        }

        // возвращаем сеанс
        return ok(schedule);
    }

    // изменить ранее созданный сеанс
    @Operation(
        summary     = "Изменить ранее созданный сеанс",
        description = "Роут изменяет сеанс в базе данных в соответствии с заданными параметрами"
    )
    @RequestBody(
        description = "Данные по сеансу",
        required    = true,
        content     = @Content(schema = @Schema(implementation = ScheduleModel.class))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос был успешно выполнен"),
        @ApiResponse(responseCode = "400", description = "Заданы некорректные входные данные запроса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Result modify(final Context ctx) {
        ScheduleModel schedule = ctx.body(ScheduleModel.class);

        // проверка корректности полученных данных
        try {
            if (schedule == null || !ScheduleModel.exists(schedule.id) || !MovieModel.exists(schedule.movieId)) {
                throw new Exception();
            }
        } catch (Exception exc) {
            return error(Code.BAD_REQUEST, "Заданы некорректные входные данные запроса!");
        }

        // сохраняем сеанс в БД
        try {
            schedule.save();
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось сохранить информацию о сеансе в базе данных!");
        }

        // сообщаем об успехе
        return ok();
    }

    // удалить сеанс по заданному идентификатору
    @Operation(
        summary     = "Удалить сеанс по заданному идентификатору",
        description = "Роут удаляет сеанс из базы данных по заданному идентификатору",
        parameters  = {
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                required = true,
                description = "Идентификатор удаляемого сеанса",
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

        // считываем идентификатор сеанса во входных параметрах
        try {
            id = Integer.parseInt(ctx.path("id").value());

            // проверяем существование сеанса по заданному идентификатору
            if (!ScheduleModel.exists(id)) {
                throw new Exception();
            }
        } catch (Exception exc) {
            return error(Code.BAD_REQUEST, "Задан некорректный идентификатор сеанса!");
        }

        // удаляем сеанс из БД
        try {
            ScheduleModel.kill(id);
        } catch (Exception exc) {
            return error(Code.INTERNAL_SERVER_ERROR, "Не удалось удалить информацию о сеансе из базы данных!");
        }

        // сообщаем об успехе
        return ok();
    }
}
