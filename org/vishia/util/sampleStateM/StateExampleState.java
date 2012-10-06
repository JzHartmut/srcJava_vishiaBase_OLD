package org.vishia.util.sampleStateM;

public class StateExampleState {
  class StateTop {
    int Off;
    class Work {
      int Ready;
      class Active{
        class Active1 {
          int Running;
          int Finit;
        }
        class Active2 {
          int RemainOn;
          int ShouldOff;
        }
      }
    }
  }       
  
  int Off(){
    return 0;
  }
  
}
