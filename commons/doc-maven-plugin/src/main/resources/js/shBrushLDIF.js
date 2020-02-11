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
 * Portions Copyright 2013-2014 ForgeRock AS
 */
;(function()
{
	// CommonJS
	SyntaxHighlighter = SyntaxHighlighter || (typeof require !== 'undefined'?
			require('shCore').SyntaxHighlighter : null);

	function Brush()
	{
		var keywords = 'add changetype control delete deleteoldrdn dn moddn ' +
		               'modify modrdn newrdn newsuperior replace version';
		
		this.regexList = [
		  { regex: new RegExp(this.getKeywords(keywords), 'gmi'),     css: 'keyword' },
		  { regex: SyntaxHighlighter.regexLib.singleLinePerlComments, css: 'comments' },
		  { regex: SyntaxHighlighter.regexLib.url,                    css: 'a' },
		  { regex: /\b(\d+\.)+\d+\b/,                                 css: 'constants' },	// OID
		  { regex: /\b(true|false)\b/,                                css: 'constants' },
		  { regex: /(:<|:&lt;|:)/,                                    css: 'color3' },		// Separator
		  { regex: /\w[\d\w-]+((;[\d\w-]))*(?=:.*)/,                  css: 'string' }		// Attr type
		];
	}

	Brush.prototype	= new SyntaxHighlighter.Highlighter();
	Brush.aliases	= ['ldif'];

	SyntaxHighlighter.brushes.LDIF = Brush;

	// CommonJS
	typeof(exports) != 'undefined' ? exports.Brush = Brush : null;
})();
