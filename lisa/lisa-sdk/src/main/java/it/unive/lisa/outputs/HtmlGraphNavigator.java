package it.unive.lisa.outputs;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.file.FileManager;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.function.Function;

public class HtmlGraphNavigator {
    private Program program;

    public HtmlGraphNavigator(Program program) {
        this.program = program;
    }

    private final static int[] illegalChars = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 34, 42, 47, 58, 60, 62, 63, 92, 124 };

    private static String cleanFileName(String name, boolean keepDirSeparator) {
        name = name.replace(' ', '_').replace("::", ".");
        // https://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars
        StringBuilder cleanName = new StringBuilder();
        int len = name.codePointCount(0, name.length());
        for (int i = 0; i < len; i++) {
            int c = name.codePointAt(i);
            // 57 is / and 92 is \
            if ((keepDirSeparator && (c == 57 || c == 92))
                    || Arrays.binarySearch(illegalChars, c) < 0)
                cleanName.appendCodePoint(c);
            else
                cleanName.appendCodePoint('_');

        }
        return cleanName.toString();
    }

    private void header(PrintWriter out){
        out.printf("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC\" crossorigin=\"anonymous\">\n" +
                "    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js\"></script>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>" + program.getName() + "</title>\n" +
                "</head>\n");
    }


    public void mkFile(Writer writer){
        String s = "";
        PrintWriter out = new PrintWriter(writer);
        header(out);


        out.printf("<body class=\"container\">\n");
        out.printf("<h1 class=\"text-center\">Analysis results</h1>\n");
        out.printf("<h4>Methods</h4>\n");

        int index = 0;
        for(CFG cfg : program.getAllCFGs()){
            out.printf(String.format("<a id=\"cfg_%d\" class=\"cfg\" href=\"./%s.html\">%s</a><br>\n", index++, cleanFileName(cfg.getDescriptor().getFullSignatureWithParNames(), true), cfg.getDescriptor().getFullSignatureWithParNames()));
        }

        out.printf("</body>\n");
    }



    public void mkCfgFile(Writer writer, CFG cfg, Function<Statement, String> labelGenerator) throws IOException {
        PrintWriter out = new PrintWriter(writer);
        header(out);
        String s = cfg.getJsonString(labelGenerator);
        out.printf("<body>\n");
        out.printf("<script src=\"https://unpkg.com/@hpcc-js/wasm@0.3.11/dist/index.min.js\"></script>\n" +
                "    <script src=\"https://d3js.org/d3.v5.min.js\"></script>\n" +
                "    <script src=\"https://unpkg.com/d3-graphviz@3.0.5/build/d3-graphviz.js\"></script>\n" +
                "\n" +
                "    <button class=\"btn btn-outline-dark position-fixed end-0 m-2\" onclick=\"toggleSidebar()\"> Settings </button>\n" +
                "        <div style=\"display: none; max-width: 25vw\" class=\"border-end position-fixed bg-light h-100\" style=\"max-width: 25%%\" data-bs-scroll=\"true\" data-bs-backdrop=\"false\" tabindex=\"-1\" id=\"sidebar\" aria-labelledby=\"sidebar\">\n" +
                "            <div class=\"offcanvas-header\">\n" +
                "                <h3 class=\"offcanvas-title\" id=\"offcanvasScrollingLabel\">Options</h3>\n" +
                "            </div>\n" +
                "            <div class=\"offcanvas-body\">\n" +
                "                <h4>Visibility settings</h4>\n" +
                "                <div>\n" +
                "                    <input type=\"checkbox\" id=\"ck_heap\" checked>\n" +
                "                    <label for=\"ck_heap\"> Show heap </label>\n" +
                "                    <br>\n" +
                "                    <input type=\"checkbox\" id=\"ck_value\" checked>\n" +
                "                    <label for=\"ck_value\"> Show value </label>\n" +
                "                    <br>\n" +
                "                    <input type=\"checkbox\" id=\"ck_expressions\" checked>\n" +
                "                    <label for=\"ck_expressions\"> Show expressions </label>\n" +
                "                    <br>\n" +
                "                    <input type=\"checkbox\" id=\"ck_interval\" checked>\n" +
                "                    <label for=\"ck_interval\"> Show interval </label>\n" +
                "                    <br>\n" +
                "                    <input type=\"checkbox\" id=\"ck_heaps\" checked>\n" +
                "                    <label for=\"ck_heaps\"> Show verbose heap </label>\n" +
                "                </div>\n" +
                "\n" +
                "                <hr>\n" +
                "\n" +
                "                <h4 style=\"text-align: center; margin: 0 0 4px;\">Intervals:</h4>\n" +
                "                <div id=\"intervalsContainer\" style=\"display: none;\">\n" +
                "                    <div id=\"intervals\">\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "\n" +
                "                <hr>\n" +
                "\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"d-flex justify-content-center w-100\">\n" +
                "\n" +
                "            <div class=\"w-100 mt-6 p-5\" id=\"graph\" style=\"text-align: center\"></div>\n" +
                "        </div>\n" +
                "\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/@popperjs/core@2.9.3/dist/umd/popper.min.js\" integrity=\"sha384-eMNCOe7tC1doHpGoWe/6oMVemdAVTMs2xqW4mwXrXsW0L84Iytr2wi5v2QjrP/xp\" crossorigin=\"anonymous\"></script>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.1.0/dist/js/bootstrap.min.js\" integrity=\"sha384-cn7l7gDp0eyniUwwAZgrzD06kc/tftFf19TOAs2zVinnD/C7E91j9yyk5//jjpt/\" crossorigin=\"anonymous\"></script>");

        String script = "    <script>\n" +
                "let input = `" + s.replace("%", "%%").replace("\"", "\\\"") +"`\n" +
                "\n" +
                "let nodes = []\n" +
                "let edges = []\n" +
                "let intervals = {}\n" +
                "let heaps = {}\n" +
                "\n" +
                "\n" +
                "const heap_checkbox = $('#ck_heap')\n" +
                "const value_checkbox = $('#ck_value')\n" +
                "const expressions_checkbox = $('#ck_expressions')\n" +
                "const interval_checkbox = $('#ck_interval')\n" +
                "const heaps_checkbox = $('#ck_heaps')\n" +
                "\n" +
                "heap_checkbox.click(e =>{\n" +
                "    updateGraph()\n" +
                "})\n" +
                "value_checkbox.click(e =>{\n" +
                "    updateGraph()\n" +
                "})\n" +
                "expressions_checkbox.click(e =>{\n" +
                "    updateGraph()\n" +
                "})\n" +
                "interval_checkbox.click(e =>{\n" +
                "    updateGraph()\n" +
                "})\n" +
                "heaps_checkbox.click(e => {\n" +
                "    updateGraph()\n" +
                "})\n" +
                "\n" +
                "\n" +
                "function parse(str) {\n" +
                "    let args = [].slice.call(arguments, 1),\n" +
                "        i = 0;\n" +
                "\n" +
                "    return str.replace(/%s/g, () => args[i++]);\n" +
                "}\n" +
                "\n" +
                "class Heap{\n" +
                "    constructor(string){\n" +
                "        this.string = string;\n" +
                "    }\n" +
                "\n" +
                "    dotFileString(){\n" +
                "        if(heaps_checkbox.is(':checked'))\n" +
                "            return this.string\n" +
                "        return this.string.replace(/'.*':/, '')\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "class Interval{\n" +
                "    constructor(id, values) {\n" +
                "        this.id = id\n" +
                "        this.low = values.low\n" +
                "        this.high = values.high\n" +
                "        if (!Object.keys(intervals).includes(id))\n" +
                "            intervals[id] = true\n" +
                "    }\n" +
                "\n" +
                "    getIdString(id){\n" +
                "        let idString;\n" +
                "        if(Object.keys(heaps).includes(id))\n" +
                "            idString = heaps[id].dotFileString()\n" +
                "        else\n" +
                "            idString = id\n" +
                "        return idString\n" +
                "    }\n" +
                "\n" +
                "    dotFileString(){\n" +
                "        if(interval_checkbox.is(':checked') && intervals[this.id])\n" +
                "            return `${this.getIdString(this.id)}: [${this.low}, ${this.high}]`\n" +
                "        return this.getIdString(this.id)\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "class Pair{\n" +
                "    constructor(left, right) {\n" +
                "        this.left = left;\n" +
                "        this.right = right\n" +
                "    }\n" +
                "\n" +
                "    dotFileString(){\n" +
                "        return `(${this.left}, ${this.right})`\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "class Edge{\n" +
                "    constructor(start, end, attributes) {\n" +
                "        this.start = start\n" +
                "        this.end = end\n" +
                "        this.attributes = attributes\n" +
                "        if (this.color === \"black\")\n" +
                "            this.type = 'sequential'\n" +
                "        if (this.color === \"red\")\n" +
                "            this.type = 'true'\n" +
                "        if (this.color === \"blue\")\n" +
                "            this.type = 'false'\n" +
                "    }\n" +
                "\n" +
                "    dotFileString() {\n" +
                "        return `\\t\"${this.start}\" -> \"${this.end}\" [color=\"${this.attributes.color}\" ${!this.attributes.style ? '' : `, style=\"${this.attributes.style}\"`}];\\n`\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "class Node{\n" +
                "    constructor(name, attributes) {\n" +
                "        attributes = JSON.parse(attributes.label.match(/{.*}/)[0])\n" +
                "\n" +
                "        this.attributes = attributes;\n" +
                "        this.name = name;\n" +
                "        this.showDetail = false\n" +
                "        this.title = '<font point-size=\"20pt\">' + attributes['instruction'].replace('>', '&gt;').replace('<', '&lt;') + '</font>'\n" +
                "        this.detail = attributes['data']\n" +
                "        this.formattedHeapAttributes = []\n" +
                "        this.formattedHeapString = this.dotFileHeap()\n" +
                "        this.formattedDetailAttributes = []\n" +
                "        this.formattedDetailString = this.dotFileValue()\n" +
                "        this.formattedExpressionsAttributes = []\n" +
                "\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    dotFileString(){\n" +
                "        return `\\t\"${this.name}\"`+\n" +
                "            ` [shape=\"${this.attributes.shape}\",`+\n" +
                "            `color=\"${this.attributes.color}\" ${!this.attributes.peripheries ? '' : `peripheries=\"${this.attributes.peripheries}\"` },`+\n" +
                "            `label=< ${this.dotFileLabel()}>];\\n`\n" +
                "    }\n" +
                "\n" +
                "    dotFileLabel(){\n" +
                "        if (this.showDetail) {\n" +
                "            let out = this.title +\n" +
                "                '<BR/>' +\n" +
                "                `${heap_checkbox.is(':checked') ? '<BR/><B>Heap: </B> ' + parse(this.dotFileHeap(), ...this.formattedHeapAttributes.map(e => e.dotFileString().replace('>', '&gt;').replace('<', '&lt;'))) : ''}` +\n" +
                "                `${value_checkbox.is(':checked') ? '<BR/><B>Value: </B> ' +  parse(this.dotFileValue(), ...this.formattedDetailAttributes.map(e => e.dotFileString().replace('>', '&gt;').replace('<', '&lt;'))) : ''}`+\n" +
                "                `${expressions_checkbox.is(':checked') ? '<BR/><B>Expressions: </B> ' +  parse(this.dotFileExpressions(), ...this.formattedExpressionsAttributes.map(e => e.dotFileString().replace('>', '&gt;').replace('<', '&lt;'))) : ''}`\n" +
                "            return out\n" +
                "        }\n" +
                "        else {\n" +
                "            return this.title\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    dotFileExpressions(){\n" +
                "        let out = \"\"\n" +
                "        this.attributes['expressions'].forEach(e =>{\n" +
                "            if(Object.keys(heaps).includes(e)){\n" +
                "                out += \"%s <BR/>\"\n" +
                "                this.formattedExpressionsAttributes.push(heaps[e])\n" +
                "            }\n" +
                "            else{\n" +
                "                out += e.replace('>', '&gt;').replace('<', '&lt;') + \"<BR/>\"\n" +
                "            }\n" +
                "        })\n" +
                "        return out;\n" +
                "    }\n" +
                "\n" +
                "    dotFileHeap(){\n" +
                "        let heap = this.detail['heap']\n" +
                "        let out = \"\"\n" +
                "        heap.forEach(e =>{\n" +
                "            if(typeof e === 'string' || e instanceof String){\n" +
                "                out += e.replace('>', '&gt;').replace('<', '&lt;') + '<BR/>';\n" +
                "            }\n" +
                "            else{\n" +
                "                let h;\n" +
                "                e['value'].forEach(v => {\n" +
                "                    if (v.match(/heap\\[.]:pp@/)){\n" +
                "                        h = new Heap(v)\n" +
                "                        heaps[v] = h;\n" +
                "                        this.formattedHeapAttributes.push(h);\n" +
                "                        out += `${e['variable'].replace('>', '&gt;').replace('<', '&lt;')}: %s<BR/>`;\n" +
                "                    } else{\n" +
                "                        out += `${e['variable'].replace('>', '&gt;').replace('<', '&lt;')}: ${v}<BR/>`;\n" +
                "                    }\n" +
                "                })\n" +
                "\n" +
                "\n" +
                "            }\n" +
                "        })\n" +
                "        return out\n" +
                "    }\n" +
                "\n" +
                "    valueString(object, isValueEnvironment = false){\n" +
                "        let out = \"\"\n" +
                "        switch (object['type']){\n" +
                "            case 'interval':{\n" +
                "                this.formattedDetailAttributes.push(new Interval(isValueEnvironment ? 'ValueEnv' : object['variable'], object))\n" +
                "                out = '%s'\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'pair':{\n" +
                "                this.formattedDetailAttributes.push(new Pair(object['left'], object['right']))\n" +
                "                out = '%s'\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'integerConstantPropagation':{\n" +
                "                out = `${object['variable'] || ''}: ${object['value']}`\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'sign':{\n" +
                "                out = `${object['variable'] || ''}: ${object['value']}`\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'nonInterference':{\n" +
                "                out = `${object['variable'] || ''}: ${object['value']}`\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'inferredState':{\n" +
                "                out = `STATE: ${object['value']}, ${object['state']}`\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'parity':{\n" +
                "                out = `${object['variable'] || ''}: ${object['value']}`\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'inferredType':{\n" +
                "                out = `${object['variable'] || ''}: ${object['value']}`\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'typeEnvironment':{\n" +
                "                out = 'TypeEnv:' + `${object['value']}`\n" +
                "                break;\n" +
                "            }\n" +
                "            case 'valueEnvironment':{\n" +
                "                out = this.valueString(object['value'], true)\n" +
                "                break;\n" +
                "            }\n" +
                "            default: out = object\n" +
                "        }\n" +
                "        return out.replace('>', '&gt;').replace('<', '&lt;') + (isValueEnvironment ? '': '<BR/>')\n" +
                "    }\n" +
                "\n" +
                "    dotFileValue(){\n" +
                "        let value = this.detail['value']\n" +
                "        let out = \"\"\n" +
                "        value.forEach(e => {\n" +
                "            out += this.valueString(e);\n" +
                "        })\n" +
                "        return out\n" +
                "    }\n" +
                "}"+
                "\n" +
                "function addIntervals(){\n" +
                "    let intervalsBox = $('#intervals');\n" +
                "    intervalsBox.empty()\n" +
                "\n" +
                "    if(Object.keys(intervals).length === 0){\n" +
                "        $('#intervalsContainer').css('display', 'none')\n" +
                "        return\n" +
                "    }else{\n" +
                "        $('#intervalsContainer').css('display', 'flex')\n" +
                "    }\n" +
                "\n" +
                "    Object.keys(intervals).sort().forEach(k => {\n" +
                "        let container = document.createElement(\"div\")\n" +
                "        let newCheck = document.createElement(\"input\")\n" +
                "        newCheck.type = 'checkbox'\n" +
                "        newCheck.id = \"int-\" + k\n" +
                "        $(newCheck).attr('checked', true)\n" +
                "\n" +
                "        let label = document.createElement('label')\n" +
                "        label.setAttribute(\"for\", \"int-\" + k);\n" +
                "        label.innerText = \"\\t\" + k;\n" +
                "\n" +
                "        $(newCheck).change(()=>{\n" +
                "            intervals[k] = !intervals[k]\n" +
                "            updateGraph()\n" +
                "        })\n" +
                "\n" +
                "        container.append(newCheck)\n" +
                "        container.append(label)\n" +
                "        intervalsBox.append(container)\n" +
                "    })\n" +
                "}\n" +
                "\n" +
                "function JSONFileAnalyzer(){\n" +
                "    nodes = []\n" +
                "    edges = []\n" +
                "    intervals = []\n" +
                "    heaps = []\n" +
                "\n" +
                "    let graph = JSON.parse(input)\n" +
                "\n" +
                "    graph.nodes.forEach(node => {\n" +
                "        nodes.push(new Node(node.id, node))\n" +
                "    })\n" +
                "\n" +
                "    graph.edges.forEach(edge => {\n" +
                "        edges.push(new Edge(edge.start, edge.end, edge))\n" +
                "    })\n" +
                "}\n" +
                "\n" +
                "function analyzeFile(){\n" +
                "    //graphMLFileAnalyzer()\n" +
                "    //dotFileAnlayzer()\n" +
                "    JSONFileAnalyzer()\n" +
                "    addIntervals()\n" +
                "}\n" +
                "\n" +
                "\n" +
                "function updateGraph(){\n" +
                "    let graph = 'digraph {\\n'\n" +
                "\n" +
                "    nodes.forEach(i =>{\n" +
                "        graph += i.dotFileString()\n" +
                "    })\n" +
                "    edges.forEach(i => {\n" +
                "        graph += i.dotFileString()\n" +
                "    })\n" +
                "\n" +
                "    graph += 'subgraph cluster_legend {\\n' +\n" +
                "        '\\tlabel=\"Legend\";\\n' +\n" +
                "        '\\tstyle=dotted;\\n' +\n" +
                "        '\\tnode [shape=plaintext];\\n' +\n" +
                "        '\\t\"legend\" [label=<<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" cellborder=\"0\"><tr><td align=\"right\">node border&nbsp;</td><td align=\"left\"><font color=\"gray\">gray</font>, single</td></tr><tr><td align=\"right\">entrypoint border&nbsp;</td><td align=\"left\"><font color=\"black\">black</font>, single</td></tr><tr><td align=\"right\">exitpoint border&nbsp;</td><td align=\"left\"><font color=\"black\">black</font>, double</td></tr><tr><td align=\"right\">sequential edge&nbsp;</td><td align=\"left\"><font color=\"black\">black</font>, solid</td></tr><tr><td align=\"right\">true edge&nbsp;</td><td align=\"left\"><font color=\"blue\">blue</font>, dashed</td></tr><tr><td align=\"right\">false edge&nbsp;</td><td align=\"left\"><font color=\"red\">red</font>, dashed</td></tr></table>>];\\n' +\n" +
                "        '}' +\n" +
                "        '}'\n" +
                "\n" +
                "    d3.select('#graph').graphviz({zoom:false}).renderDot(graph).on('end', ()=>{\n" +
                "        for(let i = 0; i<nodes.length; i++){\n" +
                "            let node = $(`#node${i+1}`)\n" +
                "            node.off()\n" +
                "            node.click(()=>{\n" +
                "                nodes[i].showDetail = !nodes[i].showDetail\n" +
                "                updateGraph()\n" +
                "            })\n" +
                "        }\n" +
                "    })\n" +
                "\n" +
                "}\n" +
                "\n" +
                "$('#sendButton').click(()=>{\n" +
                "    input = $('#dotFile').val()\n" +
                "    analyzeFile()\n" +
                "    updateGraph()\n" +
                "})\n" +
                "\n" +
                "$('#clearButton').click(()=>{\n" +
                "    $('#dotFile').val('')\n" +
                "})\n" +
                "\n" +
                "\n" +
                "let isSidebarOpen = false\n" +
                "function toggleSidebar(){\n" +
                "    if (isSidebarOpen)\n" +
                "        $('#sidebar').css('display', 'none')\n" +
                "    else\n" +
                "        $('#sidebar').css('display', 'block')\n" +
                "    isSidebarOpen = !isSidebarOpen\n" +
                "}\n"+
                "analyzeFile()\n"+
                "updateGraph()\n" +
                "</script>\n";

        out.printf(script.replace("%", "%%"));
        out.printf("</body>\n");
    }

}
