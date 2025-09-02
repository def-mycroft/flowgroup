package edge.upload.work

import core.archive.ArchivePlan
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** WorkManager stub keeping unique tasks. */
class WorkManagerStub {
    private val tasks = ConcurrentHashMap<String, ArchivePlan>()
    fun enqueueUniqueWork(name: String, plan: ArchivePlan) {
        tasks.putIfAbsent(name, plan)
    }
    fun getTasks(): Map<String, ArchivePlan> = tasks
}

/** Listener linking commit bus to WorkManagerStub. */
class CommitListener(bus: Channel<ArchivePlan>, private val wm: WorkManagerStub) {
    init {
        // Launch coroutine to consume commits
        GlobalScope.launch {
            for (plan in bus) {
                val name = "${plan.target}:${plan.sha256}"
                wm.enqueueUniqueWork(name, plan)
            }
        }
    }
}
