package org.aion.avm.core;

import java.util.*;

import static java.lang.String.format;

/**
 * Note! Nodes are double-linked parent has link to child and child has a link to the parent
 *
 * @author Roman Katerinenko
 */
public class Forest<I, C> {
    private final Collection<Node<I, C>> roots = new ArrayList<>();
    private final Map<I, Node<I, C>> nodesIndex = new HashMap<>();

    public Collection<Node<I, C>> getRoots() {
        return Collections.unmodifiableCollection(roots);
    }

    public Node<I, C> getNodeById(I id) {
        Objects.requireNonNull(id);
        return nodesIndex.get(id);
    }

    public Node<I, C> lookupNode(Node<I, C> target) {
        Objects.requireNonNull(target);
        return nodesIndex.get(target.getId());
    }

    public void add(Node<I, C> parent, Node<I, C> child) {
        Objects.requireNonNull(child);
        Objects.requireNonNull(parent);
        if (parent.getId().equals(child.getId())) {
            throw new IllegalArgumentException(format("parent(%s) id must not be equal to child id (%s)", parent.getId(), child.getId()));
        }
        Node<I, C> parentCandidate = lookupExistingFor(parent);
        boolean parentBecomesRoot = false;
        if (parentCandidate == null) {
            parentBecomesRoot = true;
            parentCandidate = parent;
            roots.add(parentCandidate);
            nodesIndex.put(parentCandidate.getId(), parentCandidate);
        }
        Node<I, C> childCandidate = lookupExistingFor(child);
        boolean childExisted = true;
        if (childCandidate == null) {
            childExisted = false;
            childCandidate = child;
            nodesIndex.put(childCandidate.getId(), childCandidate);
        }
        final var reparanting = parentBecomesRoot && childExisted;
        if (reparanting) {
            roots.remove(childCandidate);
        }
        parentCandidate.addChild(childCandidate);
        childCandidate.setParent(parentCandidate);
    }

    private Node<I, C> lookupExistingFor(Node<I, C> node) {
        return nodesIndex.get(node.getId());
    }

    public static class Node<I, C> {
        private final Collection<Node<I, C>> childs = new LinkedHashSet<>();

        private I id;
        private C content;
        private Node<I, C> parent;

        public Node(I id, C content) {
            Objects.requireNonNull(id);
            this.id = id;
            this.content = content;
        }

        public Node<I, C> getParent() {
            return parent;
        }

        public void setParent(Node<I, C> parent) {
            this.parent = parent;
        }

        public void addChild(Node<I, C> child) {
            Objects.requireNonNull(child);
            childs.add(child);
        }

        public Collection<Node<I, C>> getChildren() {
            return Collections.unmodifiableCollection(childs);
        }

        public I getId() {
            return id;
        }

        public C getContent() {
            return content;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object that) {
            return (that instanceof Node) && id.equals(((Node) that).id);
        }
    }
}