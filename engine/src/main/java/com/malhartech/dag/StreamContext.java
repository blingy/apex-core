/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.dag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the destination for tuples processed.
 *
 * @author Chetan Narsude <chetan@malhar-inc.com>
 */
public class StreamContext implements Context
{
  public static enum State
  {
    UNDEFINED,
    OUTSIDE_WINDOW,
    INSIDE_WINDOW,
    TERMINATED
  }
  private final Logger LOG = LoggerFactory.getLogger(StreamContext.class);
  private Sink sink;
  private SerDe serde;
  private int tupleCount;
  private State sinkState;

  public StreamContext()
  {
    sinkState = State.UNDEFINED;
  }

  /**
   * @param sink - target node, not required for output adapter
   */
  public void setSink(Sink sink)
  {
    LOG.debug("setSink: {}", sink);
    this.sink = sink;
  }

  public final void setSink(final Sink ultimateSink, final long fromWindowId)
  {
    LOG.debug("setSink: {} after window {}", ultimateSink, fromWindowId);
      
    sink = new Sink()
    {
      @Override
      public void doSomething(Tuple t)
      {
        if (fromWindowId <= t.getWindowId()) {
          LOG.debug("Sink {} kicking in after window {}", ultimateSink, fromWindowId);
          sink = ultimateSink;
          sink.doSomething(t);
        }
      }
    };
  }

  public SerDe getSerDe()
  {
    return serde; // required for socket connection
  }

  public void setSerde(SerDe serde)
  {
    this.serde = serde;
  }

  public void sink(Tuple t)
  {
    //LOG.info(this + " " + t);
    switch (t.getType()) {
      case SIMPLE_DATA:
      case PARTITIONED_DATA:
        tupleCount++;
        break;

      case BEGIN_WINDOW:
        tupleCount = 0;
        break;

      case END_WINDOW:
        if (tupleCount != ((EndWindowTuple) t).getTupleCount()) {
          EndWindowTuple ewt = new EndWindowTuple();
          ewt.setTupleCount(tupleCount);
          ewt.setWindowId(t.getWindowId());
          t = ewt;
          break;
        }
    }

    t.setContext(this);
    sink.doSomething(t);
  }

  public int getTupleCount()
  {
    return tupleCount;
  }

  State getSinkState()
  {
    return sinkState;
  }

  void setSinkState(State state)
  {
    sinkState = state;
  }

  @Override
  public String toString()
  {
    return " tuples = " + tupleCount + " state = " + sinkState;
  }
}
