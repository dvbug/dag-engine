package com.dvbug.dagengine.executor;

import com.dvbug.dagengine.graph.GraphData;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Stack;

@ToString
public class ExecuteContext {

    @Getter
    @ToString
    @RequiredArgsConstructor
    public static class ExecuteHistoryItem {
        private final GraphData result;
        private final String executeNode;
    }

    @Getter
    @Setter(AccessLevel.MODULE)
    @Accessors(chain = true)
    private String executeNode;
    @Getter
    @Setter(AccessLevel.MODULE)
    @Accessors(chain = true)
    private GraphData data;
    private final Stack<ExecuteHistoryItem> history = new Stack<>();

    ExecuteContext() {
    }

    ExecuteContext(@NonNull GraphData data, @NonNull String executeNode) {
        setData(data).setExecuteNode(executeNode);
    }

    ExecuteContext clone(ExecuteContext context) {
        if (null != context) {
            this.setData(context.getData()).setExecuteNode(context.getExecuteNode());
            if (!context.history.isEmpty()) {
                this.history.clear();
                context.history.forEach(this.history::push);
            }
        }
        return this;
    }

    ExecuteContext pushHistory(ExecuteHistoryItem historyItem) {
        this.history.push(historyItem);
        return this;
    }

    ExecuteHistoryItem peekHistory() {
        return this.history.peek();
    }

    public ExecuteHistoryItem[] getHistory() {
        return this.history.toArray(new ExecuteHistoryItem[0]);
    }
}
