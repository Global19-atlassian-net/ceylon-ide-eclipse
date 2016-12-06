import java.util {
    HashMap
}
import org.eclipse.debug.core.model {
    IDebugTarget,
    IProcess
}
import org.eclipse.jdt.launching {
    IVMRunner,
    IJavaLaunchConfigurationConstants,
    IVMInstall,
    VMRunnerConfiguration
}
import org.eclipse.debug.core {
    IDebugEventSetListener,
    DebugEvent,
    ILaunch,
    ILaunchConfiguration,
    DebugPlugin {
        parseArguments,
        renderArguments,
        debugPlugin = default
    }
}
import java.lang {
    ObjectArray,
    JString=String
}
import org.eclipse.core.runtime {
    IStatus,
    IProgressMonitor,
    Status,
    CoreException
}
import org.eclipse.jdt.internal.launching {
    StandardVMDebugger,
    LaunchingPlugin
}
import ceylon.interop.java {
    javaString,
    createJavaStringArray
}
import com.redhat.ceylon.eclipse.core.debug.model {
    CeylonJDIDebugTarget
}
import com.redhat.ceylon.eclipse.ui {
    CeylonPlugin {
        ceylonPluginId = pluginId,
        ceylonPlugin = instance
    }
}
import org.eclipse.jdt.debug.core {
    JDIDebugModel,
    IJavaDebugTarget,
    IJavaMethodBreakpoint
}
import org.eclipse.core.resources {
    IWorkspaceRunnable,
    ResourcesPlugin {
        workspace
    }
}
import com.sun.jdi {
    VirtualMachine
}
import org.eclipse.jdt.internal.debug.core {
    JDIDebugPlugin
}

shared interface CeylonDebuggingSupportEnabled satisfies IDebugEventSetListener {
    shared formal String getOriginalVMArguments(ILaunchConfiguration configuration);
    shared formal IVMRunner getOriginalVMRunner(ILaunchConfiguration configuration, String mode);
    
    "Should return the Java type and method"
    shared formal [String, String]? getStartLocation(ILaunchConfiguration configuration);
    
    shared formal Boolean shouldStopInMain(ILaunchConfiguration configuration);
    shared formal IVMInstall? getOriginalVMInstall(ILaunchConfiguration configuration);
    
    shared default actual void handleDebugEvents(ObjectArray<DebugEvent> events) {
        for (event in events) {
            if (event.kind == DebugEvent.create,
                is IJavaDebugTarget target = event.source,
                exists launch = target.launch,
                exists configuration = launch.launchConfiguration) {

                try {
                    if (shouldStopInMain(configuration),
                        exists [type, method] = getStartLocation(configuration)) {

                        HashMap<JString, Object> attrs = HashMap<JString, Object>();
                        value attr = javaString(IJavaLaunchConfigurationConstants.attrStopInMain);
                        attrs.put(attr, attr);
                        
                        IJavaMethodBreakpoint bp = JDIDebugModel
                                .createMethodBreakpoint(
                            workspace.root,
                            type, method, //$NON-NLS-1$
                            "()V",
                            true, false, false, -1, -1,
                            -1, 1, false, attrs); 
                        bp.persisted = false;
                        target.breakpointAdded(bp);
                        debugPlugin.removeDebugEventListener(this);
                    }
                } catch (CoreException e) {
                    LaunchingPlugin.log(e);
                }
            }
        }
    }

    throws(`class CoreException`)
    shared String getOverridenVMArguments(ILaunchConfiguration configuration)
     {
        return 
            if (exists javaDebugAgentPath = ceylonPlugin.debugAgentJar)
            then renderArguments(createJavaStringArray {
                    "-javaagent:" + javaDebugAgentPath.absolutePath,
                    for(arg in parseArguments(getOriginalVMArguments(configuration))) arg.string
            }, null)
            else getOriginalVMArguments(configuration);
    }

    throws(`class CoreException`)
    shared IVMRunner getOverridenVMRunner(ILaunchConfiguration configuration, String mode) {
        value runner = getOriginalVMRunner(configuration, mode);
        
        if (is StandardVMDebugger runner) {
            IVMInstall? vmInstall = getOriginalVMInstall(configuration);
            return object extends StandardVMDebugger(vmInstall) {
                variable IJavaDebugTarget? target = null;
                shared actual void run(VMRunnerConfiguration config, ILaunch launch, IProgressMonitor monitor) 
                {
                    try {
                        super.run(config, launch, monitor);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw CoreException(Status(IStatus.error, ceylonPluginId, "Ceylon Debugging Support Error", e));
                    }
                }
                
                shared actual IDebugTarget? createDebugTarget(
                    VMRunnerConfiguration config,
                    ILaunch launch,
                    small Integer port,
                    IProcess process, 
                    VirtualMachine vm) {
                    IWorkspaceRunnable r = object satisfies IWorkspaceRunnable {
                        shared actual void run(IProgressMonitor m) {
                            target = CeylonJDIDebugTarget(launch, vm, 
                                renderDebugTarget(config.classToLaunch, port),
                                true, false, process, config.resumeOnStartup);
                        }
                    };
                    try {
                        workspace.run(r, null, 0, null);
                    } catch (CoreException e) {
                        JDIDebugPlugin.log(e);
                    }
                    return target;
                }
            };
        } else {
            return runner;
        }
    }
}