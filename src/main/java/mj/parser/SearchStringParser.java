/* Generated By:JavaCC: Do not edit this line. SearchStringParser.java */
package mj.parser;

import java.io.StringReader;

@SuppressWarnings("all")
public class SearchStringParser implements SearchStringParserConstants {

    private Operator operator = null;

    private String fullText = null,
                   metaData= null,
                   contentData = null;

    private Boolean md = null;

    public Boolean metaDataFirst() {
      return this.md;
    }

    public Operator getOperator() {
      return this.operator;
    }

    public String getLuceneFulltextToken() {
      return this.fullText;
    }

    public String getLuceneContentToken() {
      return this.contentData;
    }

    public String getLuceneMetaDataToken() {
      return this.metaData;
    }

  final public void parse() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case CONTENTSEARCH:
      content();
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case AND_OPERATOR:
      case OR_OPERATOR:
      case AND_NOT_OPERATOR:
        op();
        metadata();
                                       this.md=false;
        break;
      case 0:
        jj_consume_token(0);
        break;
      default:
        jj_la1[0] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      break;
    case METADATASEARCH:
      metadata();
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case AND_OPERATOR:
      case OR_OPERATOR:
      case AND_NOT_OPERATOR:
        op();
        content();
                                       this.md=true;
        break;
      case 0:
        jj_consume_token(0);
        break;
      default:
        jj_la1[1] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      break;
    case FULLTEXTSEARCH:
      fulltext();
      jj_consume_token(0);
      break;
    default:
      jj_la1[2] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
  }

  final private void fulltext() throws ParseException {
                           Token luceneToken;
    jj_consume_token(FULLTEXTSEARCH);
    jj_consume_token(LUCENE_OPEN);
    luceneToken = jj_consume_token(LUCENE_EXPRESSION);
      this.fullText = luceneToken.image;
    jj_consume_token(LUCENE_CLOSE);
  }

  final private void content() throws ParseException {
                           Token luceneToken;
    jj_consume_token(CONTENTSEARCH);
    jj_consume_token(LUCENE_OPEN);
    luceneToken = jj_consume_token(LUCENE_EXPRESSION);
      this.contentData = luceneToken.image;
    jj_consume_token(LUCENE_CLOSE);
  }

  final private void metadata() throws ParseException {
                           Token luceneToken;
    jj_consume_token(METADATASEARCH);
    jj_consume_token(LUCENE_OPEN);
    luceneToken = jj_consume_token(LUCENE_EXPRESSION);
      this.metaData = luceneToken.image;
    jj_consume_token(LUCENE_CLOSE);
  }

  final private void op() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case AND_OPERATOR:
      jj_consume_token(AND_OPERATOR);
                         this.operator = Operator.AND;
      break;
    case OR_OPERATOR:
      jj_consume_token(OR_OPERATOR);
                         this.operator = Operator.OR;
      break;
    case AND_NOT_OPERATOR:
      jj_consume_token(AND_NOT_OPERATOR);
                         this.operator = Operator.AND_NOT;
      break;
    default:
      jj_la1[3] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
  }

  /** Generated Token Manager. */
  public SearchStringParserTokenManager token_source;
  JavaCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private int jj_gen;
  final private int[] jj_la1 = new int[4];
  static private int[] jj_la1_0;
  static {
      jj_la1_init_0();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {0x71,0x71,0x380,0x70,};
   }

  /** Constructor with InputStream. */
  public SearchStringParser(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public SearchStringParser(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new JavaCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new SearchStringParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 4; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 4; i++) jj_la1[i] = -1;
  }

  /** Constructor. */
  public SearchStringParser(java.io.Reader stream) {
    jj_input_stream = new JavaCharStream(stream, 1, 1);
    token_source = new SearchStringParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 4; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 4; i++) jj_la1[i] = -1;
  }

  /** Constructor with generated Token Manager. */
  public SearchStringParser(SearchStringParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 4; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(SearchStringParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 4; i++) jj_la1[i] = -1;
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[13];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 4; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 13; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

}
