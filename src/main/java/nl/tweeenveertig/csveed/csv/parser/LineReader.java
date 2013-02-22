package nl.tweeenveertig.csveed.csv.parser;

import nl.tweeenveertig.csveed.bean.instructions.CsvInstructions;
import nl.tweeenveertig.csveed.csv.structure.*;
import nl.tweeenveertig.csveed.report.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
* Builds up a List of cells (String) per read line. Note that this class is stateful, so it
* can support a per-line parse approach as well.
* @author Robert Bor
*/
public class LineReader {

    public static final Logger LOG = LoggerFactory.getLogger(LineReader.class);

    private ParseStateMachine stateMachine = new ParseStateMachine();

    private int currentLine = -1; // Haven't started yet

    private int startLine = 0;

    private int headerLine = 0;

    private Header header;

    public LineReader() {}

    public LineReader(CsvInstructions instructions) {
        this.setSymbolMapping(instructions.getSymbolMapping());
        this.getSymbolMapping().logSettings();
        this.setStartLine(instructions.getStartRow());
        LOG.info("- CSV config / start line: "+instructions.getStartRow());

        if (instructions.isUseHeader()) {
            this.setHeaderLine(instructions.getStartRow());
            LOG.info("- CSV config / has header line? yes");
        } else {
            this.setHeaderLine(-1);
            LOG.info("- CSV config / has header line? no");
        }
    }

    public List<Row> read(Reader reader) {
        List<Row> allRows = new ArrayList<Row>();
        while (!isFinished()) {
            Row row = readLine(reader);
            if (row != null && row.size() > 0) {
                allRows.add(row);
            }
        }
        return allRows;
    }

    public Row readLine(Reader reader) {
        Line unmappedLine = readBareLine(reader);
        if (unmappedLine == null) {
            return null;
        }
        if (isHeaderLine()) {
            header = new Header(unmappedLine);
            unmappedLine = readBareLine(reader);
        }
        return new RowImpl(unmappedLine, header);
    }

    public boolean isFinished() {
        return stateMachine.isFinished();
    }

    public Line readBareLine(Reader reader) {
        LineWithInfo line = new LineWithInfo();
        this.currentLine++;

        if (isBeforeStartLine()) {
            skipToStartLine(reader);
        }

        while (!stateMachine.isLineFinished()) {
            final String token;
            final int symbol;
            try {
                symbol = reader.read();
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
            try {
                token = stateMachine.offerSymbol(symbol);
            } catch (ParseException e) {
                LOG.error(e.getMessage());
                throw new CsvException(e.getMessage(), e, line.reportOnEndOfLine(), getCurrentLine());
            }
            if (stateMachine.isTokenStart()) {
                line.markStartOfColumn();
            }
            if (token != null) {
                line.addCell(token);
            }
            line.addCharacter(symbol);
        }
        line = stateMachine.isEmptyLine() ? null : line;
        stateMachine.newLine();
        return line;
    }

    private boolean isBeforeStartLine() {
        return getCurrentLine() < this.startLine;
    }

    private void skipToStartLine(Reader reader) {
        try {
            int symbol = reader.read();
            while (isBeforeStartLine() && symbol != -1) {
                if (stateMachine.isEol(symbol)) {
                    this.currentLine++;
                    if (!isBeforeStartLine()) {
                        break;
                    }
                }
                symbol = reader.read();
            }
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }

    public int getCurrentLine() {
        return this.currentLine;
    }

    public void setSymbolMapping(SymbolMapping symbolMapping) {
        this.stateMachine.setSymbolMapping(symbolMapping);
    }

    public SymbolMapping getSymbolMapping() {
        return this.stateMachine.getSymbolMapping();
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public void setHeaderLine(int headerLine) {
        this.headerLine = headerLine;
    }

    public boolean isHeaderLine() {
        return getCurrentLine() == this.headerLine;
    }

}
