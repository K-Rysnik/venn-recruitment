package ca.venn.hometask.api;

public interface VelocityLimiter {
    LoadResult attemptLoad(LoadOrder loadOrder);
}
