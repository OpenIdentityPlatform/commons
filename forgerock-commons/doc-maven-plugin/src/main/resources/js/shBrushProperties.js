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
 * SyntaxHighlighter
 * http://alexgorbatchev.com/SyntaxHighlighter
 *
 * SyntaxHighlighter is donationware. If you are using it, please donate.
 * http://alexgorbatchev.com/SyntaxHighlighter/donate.html
 *
 * @version
 * 3.0.83 (July 02 2010)
 * 
 * @copyright
 * Copyright (C) 2004-2010 Alex Gorbatchev.
 * Portions Copyright 2012 ForgeRock AS
 *
 * @license
 * Dual licensed under the MIT and GPL licenses.
 */
;(function()
{
	// CommonJS
	typeof(require) != 'undefined' ? SyntaxHighlighter =
		require('shCore').SyntaxHighlighter : null;

	function Brush()
	{
		this.regexList = [
		  { regex: /[#!;].+(\n|\r)/g,                        css: 'comments'},
		  { regex: SyntaxHighlighter.regexLib.url,           css: 'a'},
		  { regex: /[=:]/g,                                  css: 'color3'},
		  { regex: /\[\w+\]/g,                               css: 'keyword'},
		  { regex: /([^\s]|\\ )+(\s+)?(?=(=|:).+(\n|\r)?)/g, css: 'string'},
		];
	};

	Brush.prototype	= new SyntaxHighlighter.Highlighter();
	Brush.aliases	= ['ini', 'properties'];

	SyntaxHighlighter.brushes.Properties = Brush;

	// CommonJS
	typeof(exports) != 'undefined' ? exports.Brush = Brush : null;
})();
