/**
 * SyntaxHighlighter
 * http://alexgorbatchev.com/SyntaxHighlighter
 *
 * SyntaxHighlighter is donationware. If you are using it, please donate.
 * http://alexgorbatchev.com/SyntaxHighlighter/donate.html
 *
 * @version
 * 3.0.83 (Fri, 14 Mar 2014 17:58:18 GMT)
 *
 * @copyright
 * Copyright (C) 2004-2013 Alex Gorbatchev.
 *
 * @license
 * Dual licensed under the MIT and GPL licenses.
 */
/**
 * Portions Copyright 2012 ForgeRock AS
 */
;(function()
{
	// CommonJS
	typeof(require) != 'undefined' ? SyntaxHighlighter =
		require('shCore').SyntaxHighlighter : null;

	function Brush()
	{
		var keywords = 'CONNECT DELETE GET HEAD OPTIONS PATCH POST PUT TRACE';
		this.regexList = [
		  { regex: new RegExp(this.getKeywords(keywords), 'gm'), css: 'keyword'},
		  { regex: /HTTP\/\d\.\d/g,                              css: 'color2'},	// e.g. HTTP/1.1
		  { regex: /HTTP\/\d\.\d\s\d{3}\s[\w\s]+(\r|\n)/g,       css: 'color2'},	// 1st line of response
		  { regex: /:/g,                                         css: 'color3'},
		  { regex: /(\"[^\"]+\"|\'[^\']+\'|\[[^\]]+\])/g,        css: 'string'},	// Strings including [Notes]
		  { regex: /\s(\/[^\s]+)\s/g,                            css: 'color2'},	// Absolute URI
		  { regex: /[\w-]+(?=:)/g,                               css: 'constants'},	// Header names
		];
	};

	Brush.prototype	= new SyntaxHighlighter.Highlighter();
	Brush.aliases	= ['http'];

	SyntaxHighlighter.brushes.Http = Brush;

	// CommonJS
	typeof(exports) != 'undefined' ? exports.Brush = Brush : null;
})();
