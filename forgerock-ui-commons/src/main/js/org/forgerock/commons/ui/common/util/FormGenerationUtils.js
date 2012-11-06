/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global $, define*/

define("org/forgerock/commons/ui/common/util/FormGenerationUtils", [
], function () {
    
    var obj = {};
    
    obj.standardErrorSpan = '<span class="error">x</span>';
    
    obj.standardErrorMessageTag = '<div class="validation-message"></div>';
    
    obj.generateTemplateFromFormProperties = function(definition) {
        var property, formTemplate = "", formFieldType, formFieldDescription;
        for(property in definition.formProperties) {
            if (property !== '_formGenerationTemplate') {
                formFieldDescription = definition.formProperties[property];
                formFieldType = formFieldDescription.type;
                formTemplate = formTemplate + this.generateTemplateLine(property, formFieldDescription);
            }
        }
        return formTemplate;
    };
    
    obj.generateTemplateLine = function(formFieldId, formFieldDescription) {
        
        var enumValues, handlebarsValueExpression, fieldValue, valueExpression, formFieldDisplayName,
        formFieldIsReadable, formFieldIsWritable, formFieldIsRequired, formFieldType, formFieldVariableExpression, 
        formFieldVariableName, formFieldDefaultExpression, formFieldDateFormat; 
        
        formFieldIsReadable = formFieldDescription.readable;
        
        formFieldIsWritable = formFieldDescription.writable && formFieldDescription.readable;
        
        formFieldIsRequired = formFieldDescription.required && formFieldDescription.writable && formFieldDescription.readable;
        
        formFieldType = formFieldDescription.type;
        
        formFieldDisplayName = formFieldDescription.name; 
        
        formFieldVariableName = formFieldDescription.variableName ? formFieldDescription.variableName : formFieldId;
        
        formFieldVariableExpression = formFieldDescription.variableExpression ? formFieldDescription.variableExpression.expressionText : null; 
        formFieldDefaultExpression = formFieldDescription.defaultExpression ? formFieldDescription.defaultExpression.expressionText : null;
        
        if (formFieldVariableExpression) {
            valueExpression = formFieldVariableExpression;
        } else if (formFieldDefaultExpression) {
            valueExpression = formFieldDefaultExpression;
        }
        
        if (valueExpression) {
            handlebarsValueExpression = valueExpression.replace(/\$\{/g,'{{variables.');
            handlebarsValueExpression = handlebarsValueExpression.replace(/\}/g,'}}');
        }
        
        if (!formFieldType.name || formFieldType.name === 'string') {
            return this.generateStringTypeField(formFieldVariableName, formFieldDisplayName, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired);
        } else if (formFieldType.name === 'enum') {
            return this.generateEnumTypeField(formFieldVariableName, formFieldDisplayName, formFieldType.values, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired);
        } else if (formFieldType.name === 'long') {
            return this.generateLongTypeField(formFieldVariableName, formFieldDisplayName, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired);
        } else if (formFieldType.name === 'boolean') {
            return this.generateBooleanTypeField(formFieldVariableName, formFieldDisplayName, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired);
        } else if (formFieldType.name === 'date') {
            formFieldDateFormat = formFieldType.datePattern;
            return this.generateDateTypeField(formFieldVariableName, formFieldDisplayName, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired, formFieldDateFormat);
        }
    };
    
    obj.generateDateTypeField = function(elementName, elementDisplayName, value, isReadable, isWritable, isRequired, dateFormat) {
        var fieldTagStartPart = '<div class="field">', fieldTagEndPart = '</div>', label = "", input, dateFormatInput, validatorMessageTag;
        if (isReadable) {
            label = this.generateLabel(elementDisplayName);
        }
        
        if (value && value.startsWith("{{variables.")) {
            value = "{{date " + value.substring(2).removeLastChars(2) + " '" + dateFormat + "'}}";
        }      
        
        dateFormatInput = this.generateInput("dateFormat", dateFormat, false, false, false);
        input = this.generateInput(elementName, value, isReadable, isWritable, isRequired, "formattedDate");
        validatorMessageTag = isReadable && isWritable ? obj.standardErrorSpan + obj.standardErrorMessageTag : '';
        return fieldTagStartPart + label + input + validatorMessageTag + dateFormatInput + fieldTagEndPart;
    };
    
    obj.generateBooleanTypeField = function(elementName, elementDisplayName, value, isReadable, isWritable, isRequired) {
        var map = {'true' : $.t('common.form.true'), 'false' : $.t('common.form.false'), '__null' : ' '};
        return obj.generateEnumTypeField(elementName, elementDisplayName, map, value, isReadable, isWritable, isRequired);
    };
    
    obj.generateEnumTypeField = function(elementName, elementDisplayName, variableMap, value, isReadable, isWritable, isRequired) {
        var fieldTagStartPart = '<div class="field">', fieldTagEndPart = '</div>', label = '', select, additionalParams='', selectedKey, validatorMessageTag;
        
        additionalParams = isRequired ? additionalParams + ' data-validator="required" ' : '';
        additionalParams = !isWritable ? additionalParams + ' disabled="disabled" ' : additionalParams;
        additionalParams = !isReadable ? additionalParams + ' style="display: none" ' : additionalParams;
        
        selectedKey = value ? value : '__null'; 
        if (selectedKey.startsWith("{{variables.")) {
            selectedKey = selectedKey.substring(2).removeLastChars(2);
        } else {
            selectedKey = "'" + selectedKey + "'";
        }
        variableMap.__null = ' ';
        if (isReadable) {
            label = this.generateLabel(elementDisplayName);
        }
        select = "{{select '" + JSON.stringify(variableMap) + "' '" + elementName + "' " + selectedKey + " '' '" + additionalParams + "' }}";
        validatorMessageTag = isRequired && isWritable ? obj.standardErrorSpan + obj.standardErrorMessageTag : '';
        return fieldTagStartPart + label + select + validatorMessageTag + fieldTagEndPart;
    };
    
    obj.generateStringTypeField = function(elementName, elementDisplayName, handlebarsValueExpression, isReadable, isWritable, isRequired) {
        var fieldTagStartPart = '<div class="field">', fieldTagEndPart = '</div>', label = "", input, validatorMessageTag;
        if (isReadable) {
            label = this.generateLabel(elementDisplayName);
        }
        input = this.generateInput(elementName, handlebarsValueExpression, isReadable, isWritable, isRequired);
        validatorMessageTag = isRequired && isWritable ? obj.standardErrorSpan + obj.standardErrorMessageTag : '';
        return fieldTagStartPart + label + input + validatorMessageTag + fieldTagEndPart;
    };
    
    obj.generateLongTypeField = function(elementName, elementDisplayName, handlebarsValueExpression, isReadable, isWritable, isRequired) {
        var fieldTagStartPart = '<div class="field">', fieldTagEndPart = '</div>', label = "", input, validatorMessageTag;
        if (isReadable) {
            label = this.generateLabel(elementDisplayName);
        }
        input = this.generateInput(elementName, handlebarsValueExpression, isReadable, isWritable, isRequired, "long");
        validatorMessageTag = isReadable && isWritable ?  obj.standardErrorSpan + obj.standardErrorMessageTag : '';
        return fieldTagStartPart + label + input + validatorMessageTag + fieldTagEndPart;
    };
    
    obj.generateInput = function(elementName, value, isReadable, isWritable, isRequired, validatorType) {
        var isDisabledPart = isWritable ? '' : 'disabled="disabled"' , isHiddenPart = isReadable ? '' : 'style="display: none"', isRequiredPart, validatorName = 'required';
        
        if (validatorType) {
            if (isRequired) {
                validatorName = validatorName + "_" + validatorType;
            } else {
                validatorName = validatorType;
            }
        }
        isRequiredPart = isRequired || validatorType ? 'data-validator="' + validatorName + '"' : '';
        if (!value) {
            value = "";
        }
        return '<input type="text" name="' + elementName + '" value="' + value +'" ' + isDisabledPart + ' ' + isHiddenPart + ' ' + isRequiredPart + ' />';
    };
    
    obj.generateLabel = function(labelValue) {
        return '<label class="light">{{t "' + labelValue +'"}}</label>';
    };
    
    return obj;
});
    