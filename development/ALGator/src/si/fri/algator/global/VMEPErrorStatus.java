package si.fri.algator.global;

public enum VMEPErrorStatus {
    OK(0),                          // if execution was successful
    VMEPVM_ERROR(1),                 // problems with vmep vm (java.lang.UnsatisfiedLinkError, ...)
    INVALID_PROJECT(2),             // invalid project
    INVALID_ALGORITHM(3),           // invalid algorithm
    INVALID_TESTSET(4),             // invalid testset
    INVALID_ITERATOR(5),            // problems with testset iterator
    INVALID_RESULTDESCRIPTION(6),   // result description file does not exist
    
    INVALID_TEST(100),              // invalid test number
    KILLED(101),                    // test was killed
    INVALID_RESULT(102),            // invalid result in output file
    UNKNOWN(200);                   // unknown error
    
    private final int id;
    VMEPErrorStatus(int id) { this.id = id; }
    public int getValue() { return id; }

    @Override
    public String toString() {
      switch (this) {
        case OK:                          return "OK";
        case VMEPVM_ERROR:                return "Problems with vmep vm(java.lang.UnsatisfiedLinkError, ...)";
        case INVALID_PROJECT:             return "Invalid project";
        case INVALID_ALGORITHM:           return "Invalid algorithm";
        case INVALID_TESTSET:             return "Invalid testset";
        case INVALID_ITERATOR:            return "Problems with testset iterator";
        case INVALID_RESULTDESCRIPTION:   return "Result description file does not exist";
         
        case INVALID_TEST:                return "Test case was not created due to unknown error (invalid test number?).";
        case KILLED:                      return "Test was killed"; 
        case INVALID_RESULT:              return "Invalid result in output file";
        
        case UNKNOWN:                     return "Unknown error";  
        default : return "?";
      }
    }
    
    static public VMEPErrorStatus getErrorStatusByID(int id) {
      for (VMEPErrorStatus es: values()) {
        if (es.getValue() == id) return es;
      }
      return VMEPErrorStatus.UNKNOWN;
    }
  }
  