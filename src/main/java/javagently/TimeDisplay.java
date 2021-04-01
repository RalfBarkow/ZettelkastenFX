package javagently;

class TimeDisplay {

    TimeDisplay () {
      Time time, newTime;
      Display d = new Display ("Testing times");
      d.prompt("Hour",11);
      d.prompt("Min",30);
      d.prompt("add minutes",15);
      for (; ;) {
        d.ready("\nPress ready when hours and mins entered");
        int hour = d.getInt("Hour");
        int min = d.getInt("Min");
        time = new Time (hour, min);
        d.println("Time is " + time);
        time.addTime(d.getInt("add minutes"));
        d.println("and is now " + time);
     }
    }

// Put the Time class here
class Time {
    int hour;
    int min;

    Time (int h, int m) {
      hour = h;
      min = m;
    }

    Time (int mins) {
      hour = mins / 60;
      min = mins % 60;
    }

    Time (double t) {
      hour = (int) t;
      min = (int) t*100 - hour*100;
    }

    Time (Time t) {
      hour = t.hour;
      min = t.min;
    }
    void addTime (int m) {
      min +=m;
      hour+= min / 60;
      min %= 60;
    }

    boolean lessThan (Time t) {
      return hour < t.hour | (hour==t.hour & min < t.min);
    }

    public String toString () {
      return hour + ":" + min + " ";
    }

		}

  public static void main (String [] args) {
    new TimeDisplay ();
    }
  }
