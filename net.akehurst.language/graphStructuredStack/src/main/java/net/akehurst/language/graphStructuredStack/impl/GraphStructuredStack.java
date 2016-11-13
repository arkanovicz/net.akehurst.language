package net.akehurst.language.graphStructuredStack.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.akehurst.language.graphStructuredStack.IGraphStructuredStack;
import net.akehurst.language.graphStructuredStack.IGssNode;

public class GraphStructuredStack<K, V> implements IGraphStructuredStack<K, V> {

	public GraphStructuredStack() {
		this.tops = new ArrayList<>();
		this.nodes = new HashMap<>();
	}

	Map<K, IGssNode<K, V>> nodes;
	
	List<IGssNode<K, V>> tops;
	@Override
	public List<IGssNode<K, V>> getTops() {
		return this.tops;
	}
	
	@Override
	public void addTop(K key, V value) {
		IGssNode<K, V> n = this.nodes.get(key);
		if (this.getTops().contains(n)) {
			
		} else {
			if(n==null) {
				n = this.newNode(key, value);
			}
			this.getTops().add(n);
		}
	}

	@Override
	public IGssNode<K, V> newBottom(K key, V value) {
		GssNode<K, V> n = this.newNode(key, value);
		this.tops.add(n);
		return n;
	}

	GssNode<K, V> newNode(K key, V value) {
		GssNode<K, V> n = new GssNode<K, V>(this, key, value);
		this.nodes.put(key, n);
		return n;
	}
	
	@Override
	public IGssNode<K, V> peek(K key) {
		return this.nodes.get(key);
	}

	@Override
	public void pop(K key) {
		IGssNode<K, V> n = this.peek(key);
		if (this.getTops().contains(n)) {
			this.getTops().remove(n);
			this.getTops().addAll(n.previous());
		}
	}
	
	@Override
	public IGraphStructuredStack<K, V> shallowClone() {
		// TODO clone it
		return this;
	}
	
	@Override
	public String toString() {
		String out = "";
		for(IGssNode<K, V> n : this.getTops()) {
			out += n;
			while(n.hasPrevious()) {
				if (n.previous().size() > 1) {
					n = n.previous().get(0); //TODO: handle multiple
					out += " ->* " + n;					
				} else {
					n = n.previous().get(0); //TODO: handle multiple
					out += " -> " + n;
				}
			}
			out += System.lineSeparator();
		}
		return out;
	}
}
