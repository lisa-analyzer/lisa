'use strict';


  function xmlToString(xmlData) {

    var xmlString;
    //IE
    if (window.ActiveXObject) {
      xmlString = xmlData.xml;
    }
    // code for Mozilla, Firefox, Opera, etc.
    else {
      xmlString = (new XMLSerializer()).serializeToString(xmlData);
    }
    return xmlString;
  }


  function getEleData(ele) {
    var type = ele.isNode() ? "node" : "edge";
    var attrs = ["css", "data", "position"];
    var result = {};

    for (var i = 0; i < attrs.length; i++) {
      var attr = attrs[i];
      var opt = options[type][attr];
      if (!opt)
        result[attr] = {};
      else if ($.isArray(opt)) {
        result[attr] = {};
        for (var j = 0; j < opt.length; j++) {
          var el = opt[i];
          if (ele[attr](el))
            result[attr][el] = ele[attr](el);
        }
      } else {
        var eleAttr = ele[attr]();
        result[attr] = {};
        for (var key in eleAttr)
          if ($.inArray(key, options[type].discludeds) < 0) {
            result[attr][key] = {value: eleAttr[key], attrType: attr};
		  }
      }
    }

    return $.extend(result.css, result.data, result.position);
  }


  function parseNode(ele, xml) {
    var node = $('<node />', xml).attr({id: ele.id()}).appendTo(xml);

    var eleData = getEleData(ele); debugger;
    for (var key in eleData)
      $('<data />', node).attr({type: eleData[key].attrType, key: key}).text(eleData[key].value).appendTo(node);


    if (ele.isParent()) {
      var subgraph = $('<graph />', node).attr({id: ele.id() + ':'}).appendTo(node);
      ele.children().each(function (i, child) {
        parseNode(child, subgraph);
      });
    }

    return node;
  }

/*
  options.node.discludeds.push("id");
  options.edge.discludeds.push("id", "source", "target");

  var xmlDoc = $.parseXML(
          '<?xml version="1.0" encoding="UTF-8"?>\n' +
          '<graphml xmlns="http://graphml.graphdrawing.org/xmlns"\n' +
          'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n' +
          'xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns\n' +
          'http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd">\n' +
          '  <graph>\n' +
          ' </graph>\n' +
          ' </graphml>\n'
          );
  var $xml = $(xmlDoc);

  var $graph = $xml.find("graph");

  cy.nodes().orphans().forEach(function (ele) {
    parseNode(ele, $graph);
  });

  cy.edges().forEach(function (ele) {

    var edge = $('<edge />', $graph).attr({id: ele.id(), source: ele.source().id(), target: ele.target().id()}).appendTo($graph);

    var eleData = getEleData(ele);
    for (var key in eleData)
      $('<data />', edge).attr({key: key}).text(eleData[key].value).appendTo(edge);

  });


  return xmlToString(xmlDoc);
*/
  function renderNode(cy, $graph, parentNodeId) {
    $graph.children("node").each(function () {
      var $node = $(this);

      var settings = {
        data: {id: $node.attr("id"), name: $node.attr("name"), parent: parentNodeId},
        css: {},
        position: {}
      };

      $node.children('data').each(function () {
        var $data = $(this);

        //settings[$data.attr("type")][$data.attr("key")] = $data.text();
        settings.data[$data.attr("key")] = $data.text();
		
      });

      cy.add({
        group: "nodes",
        data: settings.data,
        css: settings.css,
        position: settings.position
      });

      $node.children("graph").each(function () {
        var $graph = $(this);

        renderNode(cy, $graph, $node.attr("id"));
      });
    });
  }

  function loadXml(cy, pippo, options, xmlString) {
      
      cy.batch(function () {
        var xml = $.parseXML(xmlString);
        var $xml = $(xml);

        var $graphs = $xml.find("graph");

    //    $graphs.each(function () {
    //      var $graph = $(this);
          var $graph = $graphs.first();

          renderNode(cy, $graph, undefined);

          $graph.children("edge").each(function () {
            var $edge = $(this);

            var settings = {
              data: {id: $edge.attr("id"), source: $edge.attr("source"), target: $edge.attr("target")},
              css: {},
              position: {}
            };

            $edge.children('data').each(function () {
              var $data = $(this);

              //settings[$data.attr("type")][$data.attr("key")] = $data.text();
              settings.data[$data.attr("key")] = $data.text();
            });

            cy.add({
              group: "edges",
              data: settings.data,
              css: settings.css
            });
          });

    //    });
        runLayout(cy, options);
    });
  }
  
  function runLayout(cy, options) {
    switch (typeof options.layoutBy) {
      case 'string':
        cy.layout({name: options.layoutBy}).run();
        break;
      case 'function':
        options.layoutBy();
    }
  }

  // registers the extension on a cytoscape lib ref
  var register = function (cytoscape, $) {

    if (!cytoscape || !$) {
      return;
    } // can't register if cytoscape unspecified

    var options = {
      node: {
        css: false,
        data: true,
        position: true,
        discludeds: []
      },
      edge: {
        css: false,
        data: true,
        discludeds: []
      },
      layoutBy: "cose" // string of layout name or layout function
    };

    cytoscape('core', 'graphml', function (cyGraphML) {
      var cy = this;
      var res;

      switch (typeof cyGraphML) {
        case "string": // import
          res = loadXml(cy, $, options, cyGraphML);
          break;
        case "object": // set options
          $.extend(true, options, cyGraphML);
          res = cy;
          break;
        case "undefined": // export
          res = exporter(cy, $, options);
          break;
        default:
          console.log("Functionality(argument) of .graphml() is not recognized.");
      }

      return res;

    });

  };
  
  if( typeof cytoscape !== 'undefined' && typeof jQuery !== 'undefined' ){ // expose to global cytoscape (i.e. window.cytoscape)
    register( cytoscape, jQuery );
  }

