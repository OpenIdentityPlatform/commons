/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

$(document).ready(function () {
    /* On double-click, reformat <div class="screen"> for easy copying. */
    var minusPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAAKCAYAAAC5Sw6hAAADHmlDQ1BJQ0MgUHJvZmlsZQAAeAGFVN9r01AU/tplnbDhizpnEQk+aJFuZFN0Q5y2a1e6zVrqNrchSJumbVyaxiTtfrAH2YtvOsV38Qc++QcM2YNve5INxhRh+KyIIkz2IrOemzRNJ1MDufe73/nuOSfn5F6g+XFa0xQvDxRVU0/FwvzE5BTf8gFeHEMr/GhNi4YWSiZHQA/Tsnnvs/MOHsZsdO5v36v+Y9WalQwR8BwgvpQ1xCLhWaBpXNR0E+DWie+dMTXCzUxzWKcECR9nOG9jgeGMjSOWZjQ1QJoJwgfFQjpLuEA4mGng8w3YzoEU5CcmqZIuizyrRVIv5WRFsgz28B9zg/JfsKiU6Zut5xCNbZoZTtF8it4fOX1wjOYA1cE/Xxi9QbidcFg246M1fkLNJK4RJr3n7nRpmO1lmpdZKRIlHCS8YlSuM2xp5gsDiZrm0+30UJKwnzS/NDNZ8+PtUJUE6zHF9fZLRvS6vdfbkZMH4zU+pynWf0D+vff1corleZLw67QejdX0W5I6Vtvb5M2mI8PEd1E/A0hCgo4cZCjgkUIMYZpjxKr4TBYZIkqk0ml0VHmyONY7KJOW7RxHeMlfDrheFvVbsrj24Pue3SXXjrwVhcW3o9hR7bWB6bqyE5obf3VhpaNu4Te55ZsbbasLCFH+iuWxSF5lyk+CUdd1NuaQU5f8dQvPMpTuJXYSWAy6rPBe+CpsCk+FF8KXv9TIzt6tEcuAcSw+q55TzcbsJdJM0utkuL+K9ULGGPmQMUNanb4kTZyKOfLaUAsnBneC6+biXC/XB567zF3h+rkIrS5yI47CF/VFfCHwvjO+Pl+3b4hhp9u+02TrozFa67vTkbqisXqUj9sn9j2OqhMZsrG+sX5WCCu0omNqSrN0TwADJW1Ol/MFk+8RhAt8iK4tiY+rYleQTysKb5kMXpcMSa9I2S6wO4/tA7ZT1l3maV9zOfMqcOkb/cPrLjdVBl4ZwNFzLhegM3XkCbB8XizrFdsfPJ63gJE722OtPW1huos+VqvbdC5bHgG7D6vVn8+q1d3n5H8LeKP8BqkjCtbCoV8yAAAAUklEQVQoFWO8HyrynwEKFFa9ZoSxiaEfhInC9TKADAICBkowyAwmYmwmRg3VDGLBZtvHPGmE37Eo4J/0FCMsaesibDZicRiKENVcRDWDGKmVIAFsW0aIsyjVzwAAAABJRU5ErkJggg%3D%3D";
    var minus = "<img alt=\"[-]\" class=\"toggle\" width=\"18\" height=\"10\" src=\"" + minusPng + "\">";
    var plusPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAAKCAYAAAC5Sw6hAAADHmlDQ1BJQ0MgUHJvZmlsZQAAeAGFVN9r01AU/tplnbDhizpnEQk+aJFuZFN0Q5y2a1e6zVrqNrchSJumbVyaxiTtfrAH2YtvOsV38Qc++QcM2YNve5INxhRh+KyIIkz2IrOemzRNJ1MDufe73/nuOSfn5F6g+XFa0xQvDxRVU0/FwvzE5BTf8gFeHEMr/GhNi4YWSiZHQA/Tsnnvs/MOHsZsdO5v36v+Y9WalQwR8BwgvpQ1xCLhWaBpXNR0E+DWie+dMTXCzUxzWKcECR9nOG9jgeGMjSOWZjQ1QJoJwgfFQjpLuEA4mGng8w3YzoEU5CcmqZIuizyrRVIv5WRFsgz28B9zg/JfsKiU6Zut5xCNbZoZTtF8it4fOX1wjOYA1cE/Xxi9QbidcFg246M1fkLNJK4RJr3n7nRpmO1lmpdZKRIlHCS8YlSuM2xp5gsDiZrm0+30UJKwnzS/NDNZ8+PtUJUE6zHF9fZLRvS6vdfbkZMH4zU+pynWf0D+vff1corleZLw67QejdX0W5I6Vtvb5M2mI8PEd1E/A0hCgo4cZCjgkUIMYZpjxKr4TBYZIkqk0ml0VHmyONY7KJOW7RxHeMlfDrheFvVbsrj24Pue3SXXjrwVhcW3o9hR7bWB6bqyE5obf3VhpaNu4Te55ZsbbasLCFH+iuWxSF5lyk+CUdd1NuaQU5f8dQvPMpTuJXYSWAy6rPBe+CpsCk+FF8KXv9TIzt6tEcuAcSw+q55TzcbsJdJM0utkuL+K9ULGGPmQMUNanb4kTZyKOfLaUAsnBneC6+biXC/XB567zF3h+rkIrS5yI47CF/VFfCHwvjO+Pl+3b4hhp9u+02TrozFa67vTkbqisXqUj9sn9j2OqhMZsrG+sX5WCCu0omNqSrN0TwADJW1Ol/MFk+8RhAt8iK4tiY+rYleQTysKb5kMXpcMSa9I2S6wO4/tA7ZT1l3maV9zOfMqcOkb/cPrLjdVBl4ZwNFzLhegM3XkCbB8XizrFdsfPJ63gJE722OtPW1huos+VqvbdC5bHgG7D6vVn8+q1d3n5H8LeKP8BqkjCtbCoV8yAAAAYElEQVQoFWO8HyrynwEKFFa9ZoSxiaEfhInC9TKADAICBlz4Q67UfxDGJQ8SB5nBRIzNxKihmkEs2Gz7mCeN8DtUAbIY/6SnGGFJWxch2whzCbIYNl9QzUVUM4iRWgkSAKcsS/O6iJF0AAAAAElFTkSuQmCC";
    var plus = "<img alt=\"[+]\" class=\"toggle\" width=\"18\" height=\"10\" src=\"" + plusPng + "\">";

    /*
     * DOCS-36: HTML output should wrap large images in links to themselves at
     *          full size
     *
     * It would be cool if when you click (large) images in HTML output, you get
     * the same effect as right-click &gt; copy image location + new tab &gt;
     * paste copied image location and go.
     *
     * Run this before inserting [-] and [+] images below, otherwise clicking
     * [-] opens a new browser tab with the [-] image.
     */
    $("img").wrap(function () {
        return "<a target=\"_blank\" href=\"" + $(this).prop('src') + "\" />";
    });

    /*
     * COMMWEB-73: Make it easier to copy links to anchors in the HTML core docs
     *
     * On mouseover, prepend a ↪ to titles which links to the element.
     *
     * DOCS-85: Make it easy to get the link to other titled block elements
     *
     * It appears that the classes for all title elements do end in "title".
     * Some of the anchors are children, but some are siblings. And some just
     * do not have name anchors.
     *
     * DOCS-197: Prepending ↪ on mouseover is distracting
     *
     * Instead of prepending a ↪, append a link symbol.
     *
     */
    $("[class$=title]").each(function () {
        var href = $(this).children("a").prop("name");
        if (href === undefined) {
            href = $(this).siblings("a").prop("name");
        }

        if (href === undefined || href === "") { // Name anchor seems to be missing.
            return;
        }

        $(this).append('<span class=\"showlink\" ' +
                             'title=\"Scroll heading to top of page\"> &#x2693;</span>');
        $(this).wrap('<a href=\"#' + href + '\"></a>');
        $(this).mouseenter(function () {
            $(this).children('span.showlink').show();
        }).mouseleave(function () {
            $(this).children('span.showlink').hide();
        });
    });

    /*
     * DOCS-57: Add collapse/expand chapter in single-page HTML
     *
     * Use [-] image to mean "collapse" and [+] image to mean "expand".
     */
    var tocollapse = minus.replace(/toggle/, "tocollapse");
    var toexpand = plus.replace(/toggle/, "toexpand");

    $(".titlepage").css("clear", "left");
    /* Fix for DOCS-70 */

    $("div.preface,div.chapter,div.reference,div.appendix,div.glossary,div.index").each(function () {
        $(this).prepend(tocollapse + toexpand);
    });
    $(".toexpand").hide();

    $(".tocollapse").click(function () {
        $(this).siblings(":not(.titlepage)").hide();
        $(this).siblings(".toexpand").show();
        $(this).hide();
    });

    $(".toexpand").click(function () {
        $(this).siblings("*").show();
        $(this).hide();
    });

    var bookcollapse = minus.replace(/toggle/, "bookcollapse");
    var bookexpand = plus.replace(/toggle/, "bookexpand");
    $("div.book").prepend(bookcollapse + bookexpand);
    $(".bookexpand").hide();

    $(".bookcollapse").click(function () {
        $(".tocollapse").click();
        $(".bookexpand").show();
        $(this).hide();
    });

    $(".bookexpand").click(function () {
        $(".toexpand").click();
        $(".bookcollapse").show();
        $(this).hide();
    });

    /*
     * DOCS-117: Advertise latest stable doc release in in-progress documentation
     *
     * If the projectName/projectVersion is not equal to
     * the latest published projectName/projectVersion,
     * then show a link.
     */
    var project = "PROJECT_NAME";       // E.g. "OpenAM"
    var version = "PROJECT_VERSION";    // E.g. "11.0.0"
    var jsonUrl = "LATEST_JSON";        // E.g. "http://docs.forgerock.org/latest.json"
    var language = "en";
    var latestUrl, latest;

    $.getJSON(jsonUrl, function( data ) {
        latest = data[project];
        latestUrl = "DOCS_SITE" + language + "/" + project + "/" + latest + "/";

        if (latest !== version) {
            $("<div><p class=\"latest\"><a href=\"" + latestUrl +
                "\">Latest release: " + latest + "</a></p></div>").
                appendTo("body");
        }
    });

    /*
     * DOCS-187: Documentation for legacy versions should not have the report bug footer
     *
     * If the projectName/projectVersion is EOSL, then hide the footer,
     * which makes it easy to file JIRA issues on this documentation.
     */
    jsonUrl = "EOSL_JSON";        // E.g. "http://docs.forgerock.org/eosl.json"

    $.getJSON(jsonUrl, function( data ) {
        var eoslVersions = data[project];

        if (eoslVersions.indexOf(version) !== -1) { // Matched EOSL version.
            $("#footer").addClass("nodisplay");
        }
    });
});
