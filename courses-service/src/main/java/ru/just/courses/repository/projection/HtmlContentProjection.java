package ru.just.courses.repository.projection;

import java.sql.Clob;

public interface HtmlContentProjection {
    Clob getHtml();
}
