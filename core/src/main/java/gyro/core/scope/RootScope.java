package gyro.core.scope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gyro.core.Credentials;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceFinder;
import gyro.core.workflow.Workflow;
import gyro.lang.ast.block.VirtualResourceNode;

public class RootScope extends FileScope {

    private final RootScope current;
    private final Map<String, Class<?>> resourceClasses = new HashMap<>();
    private final Map<String, Class<? extends ResourceFinder>> resourceFinderClasses = new HashMap<>();
    private final Map<String, VirtualResourceNode> virtualResourceNodes = new LinkedHashMap<>();
    private final List<Workflow> workflows = new ArrayList<>();
    private final List<FileScope> fileScopes = new ArrayList<>();
    private final Map<String, Resource> resources = new LinkedHashMap<>();
    private final Set<String> activeScopePaths = new HashSet<>();
    private final Map<String, Set<String>> duplicateResources = new HashMap<>();

    public RootScope(String file) {
        this(file, null, Collections.emptySet());
    }

    public RootScope(RootScope current) {
        this(current.getFile(), current, Collections.emptySet());
    }

    public RootScope(String file, Set<String> activePaths) {
        this(file, null, activePaths);
    }

    public RootScope(RootScope current, Set<String> activePaths) {
        this(current.getFile(), current, activePaths);
    }

    private RootScope(String file, RootScope current, Set<String> activePaths) {
        super(null, file);
        this.current = current;

        put("ENV", System.getenv());

        try {
            Path rootPath = Paths.get(file).toAbsolutePath().getParent();
            try (Stream<Path> pathStream = getCurrent() != null
                ? Files.find(rootPath.getParent(), 100,
                    (p, b) -> b.isRegularFile()
                        && p.toString().endsWith(".gyro")
                        && !p.toString().startsWith(rootPath.toString()))

                : Files.find(rootPath, 100,
                    (p, b) -> b.isRegularFile()
                        && p.toString().endsWith(".gyro.state"))) {

                for (Path path : pathStream.collect(Collectors.toSet())) {
                    FileScope fileScope = new FileScope(this, path.toString());
                    getFileScopes().add(fileScope);
                }
            }

            if (getCurrent() == null) {
                for (String path : activePaths) {
                    path += ".state";
                    Path rootDir = GyroCore.findPluginPath().getParent().getParent();
                    Path relative = rootDir.relativize(Paths.get(path).toAbsolutePath());
                    Path statePath = Paths.get(rootDir.toString(), ".gyro", "state", relative.toString());
                    Files.createDirectories(statePath.getParent());

                    this.activeScopePaths.add(statePath.toString());
                }
            } else {
                this.activeScopePaths.addAll(activePaths);
            }

        } catch (IOException e) {
            throw new GyroException(e.getMessage(), e);
        }

        put("ENV", System.getenv());
    }

    public RootScope getCurrent() {
        return current;
    }

    public Map<String, Class<?>> getResourceClasses() {
        return resourceClasses;
    }

    public Map<String, Class<? extends ResourceFinder>> getResourceFinderClasses() {
        return resourceFinderClasses;
    }

    public Map<String, VirtualResourceNode> getVirtualResourceNodes() {
        return virtualResourceNodes;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public List<FileScope> getFileScopes() {
        return fileScopes;
    }

    public void putResource(String name, Resource resource) {
        if (resources.containsKey(name)) {
            Resource old = resources.get(name);
            String oldPath = old.scope().getFileScope().getFile();
            String path = resource.scope().getFileScope().getFile();
            if (!oldPath.equals(path)) {
                duplicateResources.putIfAbsent(name, new HashSet<>());
                duplicateResources.get(name).add(oldPath);
                duplicateResources.get(name).add(path);
            }
        }

        resources.put(name, resource);
    }

    public Set<String> getActiveScopePaths() {
        return activeScopePaths;
    }

    public List<Resource> findAllResources() {
        return new ArrayList<>(resources.values());
    }

    public List<Resource> findAllActiveResources() {

        if (getActiveScopePaths().isEmpty()) {
            return findAllResources();
        }

        try {
            List<FileScope> activeFileScopes = new ArrayList<>();
            for (FileScope fileScope : getFileScopes()) {
                for (String path : activeScopePaths) {
                    if (Files.isSameFile(Paths.get(fileScope.getFile()), Paths.get(path))) {
                        activeFileScopes.add(fileScope);
                    }
                }
            }

            return resources.values().stream()
                .filter(r -> activeFileScopes.contains(r.scope().getFileScope()))
                .collect(Collectors.toList());

        } catch (IOException e) {
            throw new GyroException(e.getMessage(), e);
        }
    }

    public Resource findResource(String name) {
        return resources.get(name);
    }

    public void validate() {
        StringBuilder sb = new StringBuilder();
        for (FileScope fileScope : getFileScopes()) {
            boolean hasCredentials = fileScope.values()
                .stream()
                .anyMatch(Credentials.class::isInstance);

            if (hasCredentials) {
                sb.append(String.format("Credentials are only allowed in '%s', found in '%s'%n", getFile(), fileScope.getFile()));
            }
        }

        boolean hasResources = this.values()
            .stream()
            .anyMatch(r -> r instanceof Resource && !(r instanceof Credentials));

        if (hasResources) {
            sb.append(String.format("Resources are not allowed in '%s'%n", getFile()));
        }

        for (Map.Entry<String, Set<String>> entry : duplicateResources.entrySet()) {
            sb.append(String.format("%nDuplicate resource %s defined in the following files:%n", entry.getKey()));
            entry.getValue().stream()
                .map(p -> p + "\n")
                .forEach(sb::append);
        }

        if (sb.length() != 0) {
            sb.insert(0, "Invalid configs\n");
            throw new GyroException(sb.toString());
        }
    }
}
