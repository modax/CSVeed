package nl.tweeenveertig.csveed.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

import static nl.tweeenveertig.csveed.token.EncounteredSymbol.*;

public class SymbolMapping {

    public static final Logger LOG = LoggerFactory.getLogger(SymbolMapping.class);

    private Map<EncounteredSymbol, char[]> symbolToChars = new TreeMap<EncounteredSymbol, char[]>();
    private Map<Character, EncounteredSymbol> charToSymbol = new TreeMap<Character, EncounteredSymbol>();

    private Character escapeCharacter;

    private Character quoteCharacter;

    public SymbolMapping() {
        initDefaultMapping();
    }

    public void initDefaultMapping() {
        addMapping(EncounteredSymbol.ESCAPE_SYMBOL, '"');
        addMapping(EncounteredSymbol.QUOTE_SYMBOL, '"');
        addMapping(EncounteredSymbol.SEPARATOR_SYMBOL, ';');
        addMapping(EncounteredSymbol.EOL_SYMBOL, new char[]{ '\r', '\n' } );
        addMapping(EncounteredSymbol.SPACE_SYMBOL, ' ');
    }

    public void addMapping(EncounteredSymbol symbol, Character character) {
        addMapping(symbol, new char[] { character } );
        if (symbol.isCheckForSimilarEscapeAndQuote()) {
            storeCharacterForLaterComparison(symbol, character);
        }
    }

    public void addMapping(EncounteredSymbol symbol, char[] characters) {
        for (Character character : characters) {
            charToSymbol.put(character, symbol);
        }
        symbolToChars.put(symbol, characters);
    }

    public void logSettings() {
        for (EncounteredSymbol symbol : symbolToChars.keySet()) {
            char[] characters = symbolToChars.get(symbol);
            LOG.info("- CSV config / Characters for "+symbol+" "+charactersToString(characters));
        }
    }

    private String charactersToString(char[] characters) {
        StringBuilder returnString = new StringBuilder();
        for (char currentChar : characters) {
            returnString.append(charToPrintable(currentChar));
            returnString.append(" ");
        }
        return returnString.toString();
    }

    private String charToPrintable(char character) {
        switch(character) {
            case '\t' : return "\\t";
            case '\n' : return "\\n";
            case '\r' : return "\\r";
            default: return Character.toString(character);
        }
    }

    private void storeCharacterForLaterComparison(EncounteredSymbol symbol, Character character) {
        if (symbol == ESCAPE_SYMBOL) {
            escapeCharacter = character;
        } else if (symbol == QUOTE_SYMBOL) {
            quoteCharacter = character;
        }
    }

    public boolean isSameCharactersForEscapeAndQuote() {
        return escapeCharacter != null && quoteCharacter != null && escapeCharacter.equals(quoteCharacter);
    }

    public EncounteredSymbol find(int character, ParseState parseState) {
        if (character == -1) {
            return END_OF_FILE_SYMBOL;
        }
        EncounteredSymbol symbol = charToSymbol.get((char)character);
        if (symbol == null) {
            return OTHER_SYMBOL;
        }
        if (symbol.isCheckForSimilarEscapeAndQuote() && isSameCharactersForEscapeAndQuote()) {
            return parseState.isUpgradeQuoteToEscape() ? ESCAPE_SYMBOL : QUOTE_SYMBOL;
        } else {
            return symbol;
        }
    }

}