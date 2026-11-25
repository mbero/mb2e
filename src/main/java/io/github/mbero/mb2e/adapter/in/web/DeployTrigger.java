package io.github.mbero.mb2e.adapter.in.web;

/** Accepts a trigger for a deployment and tells what happened to it. */
public interface DeployTrigger {

    /** Starts a run now, lets it wait, merges it into the waiting run, or drops it while the service stops. */
    DeployQueue.Result submit(String reason);
}
