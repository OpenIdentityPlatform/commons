/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define */

define("org/forgerock/commons/ui/common/util/BackgridUtils", [
    "jquery",
    "underscore",
    "backgrid",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "moment",
    "backgrid-filter"
], function ($, _, Backgrid, DateUtil, UIUtils, moment) {
    var obj = {};
    
    /**
     * @exports org/forgerock/commons/ui/common/util/BackgridUtils
     */
    
    /**
    * Makes the provided table drag and droppable
    *
    * @param {object} data
    * @param {object} data.grid - jQuery ref to a table
    * @params {array} data.rows - array of the rows in the table
    * @param {object} callback - called on row drop
    * 
    */
    obj.sortable = function(data, callback) {
        if (data.grid && data.rows.length > 0) {

            var offset = 0,
                bottomBounds = 0,
                topBounds = 0,
                startIndex = -1,
                table;

            /*
             * TODO: when jQuery UI is removed from the stack, we need to go back to using 
             *       the plain "sortable" plugin instead of "nestingSortable"
             */
            data.grid.nestingSortable({
                containerSelector: 'table',
                itemPath: '> tbody',
                itemSelector: 'tr',
                placeholder: '<tr class="placeholder"/>',
                onMousedown: function ($item, _super, event) {
                    table = $item.closest(this.containerSelector);
                    topBounds = table.offset().top;
                    bottomBounds = topBounds + table.height();

                    offset = event.offsetY;

                    startIndex = table.find("tbody tr").index($item);

                    // set a fixed width of all cells so that when dragging, our cells width doesn't collapse
                    $('td, th', 'table').each(function () {
                        var cell = $(this);
                        cell.width(cell.width());
                    });

                    if (!event.target.nodeName.match(/^(input|select)$/i)) {
                        event.preventDefault();
                        return true;
                    }
                },

                onDrag: function ($item, position, _super, event) {
                    if (position.top - offset >= topBounds && position.top - offset <= bottomBounds) {
                        $item.css("top", position.top - offset);
                    }
                },

                onDrop: function ($item, container, _super, event) {
                    var endIndex = table.find("tbody tr").index($item),
                        tempCopy;

                    if (startIndex >= 0 && endIndex >= 0) {
                        tempCopy = data.rows[startIndex];
                        data.rows.splice(startIndex, 1);
                        data.rows.splice(endIndex, 0, tempCopy);
                    }

                    // remove fixed width so that if content/table is resized then
                    $('td, th', 'table').each(function () {
                        var cell = $(this);
                        cell.css('width', '');
                    });

                    _super($item, container, _super, event);

                    if (callback) {
                        callback(data.rows);
                    }
                }
            });
        }
    };

    obj.formatDate = function(date) {
        var returnDate = "";
        if(date) {
            returnDate = DateUtil.formatDate(date, "MMM dd, yyyy") +
            " <small class='text-muted'>" +
            DateUtil.formatDate(date, "h:mm:ss TT") +
            "</small>";
        }

        return returnDate;
    };

    /**
     * The date cell will search the model attributes for the provided property and format that into a standard date.
     * @param dateProperty{string}
     * @returns {*}
     */
    obj.DateCell = function (dateProperty) {
        var _this = this;
        return Backgrid.Cell.extend({
            render: function () {
                if (this.model.get(dateProperty)) {
                    this.$el.html(_this.formatDate(this.model.get(dateProperty)));
                } else {
                    this.$el.html("");
                }
                return this;
            }
        });
    };
    
    /**
     * Datetime Ago Cell Renderer
     * <p>
     * Displays human friendly date time text (e.g. 4 "hours ago") with a tooltip of the exact time
     */
    obj.DatetimeAgoCell = Backgrid.Cell.extend({
        className: "date-time-ago-cell",
        formatter: {
            fromRaw: function (rawData, model) {
                return moment(rawData).fromNow();
            }
        },
        render: function () {
            Backgrid.Cell.prototype.render.apply(this);
            this.$el.attr("title", moment(this.model.get(this.column.get("name"))).format("Do MMMM YYYY, h:mm:ssa"));
            return this;
        }
    });

    /**
     * The button cell allows you to define an array of icons to insert into a single cell.
     * The icons will be given the class name and will execute the callback on click.
     *
     * @param buttons {array}
     *      EXAMPLE:
     *       cell: CustomCells.ButtonCell([{
     *           className: "fa fa-pencil grid-icon",
     *           callback: function(){alert(this.model.get("createTime"));}
     *       }, {
     *           className: "fa fa-plus grid-icon",
     *           callback: function(){alert(this.model.get("assignee"));}
     *       }])
     * @returns {Backgrid.Cell}
     */
    obj.ButtonCell = function (buttons, renderCallback) {
        var events = {},
            html = "";

        _.each(buttons, function(button, index) {
            if(button.href) {
                html += ("<a href=\"" +button.href +"\"><i class=\"button-" + index + " " + button.className +  "\"></i></a>");
            } else {
                events["click .button-"+index] = button.callback;
                html += ("<i class=\"button-" + index + " " + button.className +  "\"></i>");
            }
        });

        return Backgrid.Cell.extend({
            events: events,

            render: function () {
                this.$el.html(html);
                this.delegateEvents();

                if (renderCallback) {
                    _.bind(renderCallback, this)();
                }
                return this;
            }
        });
    };

    /**
     * In the case that a grid needs to sort on a property other than the one displayed, use this custom cell.
     * EXAMPLE: Will sort on "taskName" and display "name"
     *    {
     *        label: "Task",
     *        name: "taskName",
     *        cell: CustomCells.DisplayNameCell(name),
     *        sortable: true,
     *        editable: false
     *    }
     * @param displayProperty
     * @returns {*}
     */
    obj.DisplayNameCell = function (displayProperty) {
        return Backgrid.Cell.extend({
            render: function () {
                this.$el.text(this.model.get(displayProperty));
                return this;
            }
        });
    };

    /**
     * addSmallScreenCell creates a hidden column with a custom "smallScreenCell" that
     * will be displayed as a replacement for the full grid on small screens. It takes
     * an array of Backgrid column definitions, loops over them, adds a vertical
     * representation of how the cell is rendered for the current column to the
     * smallScreenCell's html, then adds the newly created column definition to the
     * originally defined grid columns.
     *
     * the "hideColumnLabel" param can be passed in to display the cell with no label
     * for the associated value
     *
     * @param cols {array}
     * @param hideColumnLabels {boolean}
     * @returns {array}
     *
     */

    obj.addSmallScreenCell = function (cols, hideColumnLabels) {
        var smallScreenCell = Backgrid.Cell.extend({
            className: "smallScreenCell",
            events: {},
            render: function () {
                var html = "",
                    filteredCols = _.reject(cols, function (c) {
                        return c.name === "smallScreenCell";
                    });

                _.each(filteredCols, _.bind(function (col) {
                    var cellView,
                        label = "<span class='text-muted'>" + col.label + ":</span> ",
                        cellWrapper;

                    if (_.isObject(col.cell)) {
                        cellView = new col.cell({ model: this.model, column: col});
                        cellView.$el = $("<span>");
                        cellView.render();

                        if (!_.isEmpty(_.omit(cellView.events, "click"))) {
                            cellWrapper = $("<p class='pull-right show'></p>");

                            if (cellView.$el.html().length && !hideColumnLabels && col.label) {
                                cellWrapper.append(label);
                            }

                            cellWrapper.append(cellView.$el);

                            this.$el.prepend(cellWrapper);
                        } else {
                            cellWrapper = $("<p>");

                            if (cellView.$el.html().length && !hideColumnLabels && col.label) {
                                cellWrapper.append(label);
                            }

                            cellWrapper.append(cellView.$el);

                            this.$el.append(cellWrapper);
                        }
                    } else {
                        cellWrapper = $("<p>");
                        if (this.model.get(col.name) && this.model.get(col.name).length && !hideColumnLabels && col.label) {
                            cellWrapper.append(label);
                        }

                        cellWrapper.append(this.model.get(col.name));

                        this.$el.append(cellWrapper);
                    }
                }, this));

                return this;
            }
        }),
        newCol = {
            name: "smallScreenCell",
            editable: false,
            sortable: false,
            cell: smallScreenCell
        };

        cols.push(newCol);

        return cols;
    };
    
    
    /**
     * Handlebars Template Cell Renderer
     * <p>
     * You must extend this renderer and specify a "template" attribute e.g.
     * <p>
     * MyCell = backgridUtils.TemplateCell.extend({
     *     template: "templates/MyTemplate.html"
     * });
     */
    obj.TemplateCell = Backgrid.Cell.extend({
        className: 'template-cell',

        events: {
            'click': '_onClick'
        },

        render: function () {
            this.$el.html(UIUtils.fillTemplateWithData(this.template, this.model.attributes));

            if (this.additionalClassName) {
                this.$el.addClass(this.additionalClassName);
            }

            if (this.callback) {
                this.callback();
            }

            this.delegateEvents();

            return this;
        },

        _onClick: function (e) {
            if (this.onClick) {
                this.onClick(e, this.model.id);
            }
        }
    });
    

    /**
     * Object Cell
     * <p>
     * Displays cell content as a definition list. Used for cells which values are objects
     */
    obj.ObjectCell = Backgrid.Cell.extend({
        className: "object-formatter-cell",

        render: function () {
            this.$el.empty();

            var object = this.model.get(this.column.attributes.name),
                result = '<dl class="dl-horizontal">',
                prop;

            for (prop in object) {
                if (_.isString(object[prop])) {
                    result += "<dt>" + prop + "</dt><dd>" + object[prop] + "</dd>";
                } else {
                    result += "<dt>" + prop + "</dt><dd>" + JSON.stringify(object[prop]) + "</dd>";
                }
            }
            result += "</dl>";

            this.$el.append(result);

            this.delegateEvents();
            return this;
        }
    });

    /**
     * Array Cell
     * <p>
     * Displays cell content as an unordered list. Used for cells which values are arrays
     */
    obj.ArrayCell = Backgrid.Cell.extend({
        className: "array-formatter-cell",

        buildHtml: function (arrayVal) {
            var result = "<ul>",
                i = 0;

            for (; i < arrayVal.length; i++) {
                if (_.isString(arrayVal[i])) {
                    result += "<li>" + arrayVal[i] + "</li>";
                } else {
                    result += "<li>" + JSON.stringify(arrayVal[i]) + "</li>";
                }
            }
            result += "</ul>";

            return result;
        },

        render: function () {
            this.$el.empty();

            var arrayVal = this.model.get(this.column.attributes.name);
            this.$el.append(this.buildHtml(arrayVal));

            this.delegateEvents();
            return this;
        }
    });

    obj.FilterHeaderCell = Backgrid.HeaderCell.extend({
        className: "filter-header-cell",
        render: function () {
            var filter = new Backgrid.Extension.ServerSideFilter({
                name: this.column.get("name"),
                placeholder: $.t("common.form.filter"),
                collection: this.collection
            });

            if (this.addClassName) {
                this.$el.addClass(this.addClassName);
            }

            this.collection.state.filters = this.collection.state.filters ? this.collection.state.filters : [];
            this.collection.state.filters.push(filter);
            obj.FilterHeaderCell.__super__.render.apply(this);
            this.$el.prepend(filter.render().el);
            return this;
        }
    });

    /**
     * Clickable Row
     * <p>
     * You must extend this row and specify a "callback" attribute e.g.
     * <p>
     * MyRow = BackgridUtils.ClickableRow.extend({
     *     callback: myCallback
     * });
     */
    obj.ClickableRow = Backgrid.Row.extend({
        events: {
            "click": "onClick"
        },

        onClick: function (e) {
            if (this.callback) {
                this.callback(e);
            }
        }
    });

    obj.sortKeys = function () {
        return this.state.order === 1 ? '-' + this.state.sortKey : this.state.sortKey;
    };

    // FIXME: Workaround to fix "Double sort indicators" issue
    // @see https://github.com/wyuenho/backgrid/issues/453
    obj.doubleSortFix = function (model) {
        // No ids so identify model with CID
        var cid = model.cid,
            filtered = model.collection.filter(function (model) {
                return model.cid !== cid;
            });

        _.each(filtered, function (model) {
            model.set('direction', null);
        });
    };

    obj.pagedResultsOffset = function () {
        return (this.state.currentPage - 1) * this.state.pageSize;
    };

    return obj;

});
