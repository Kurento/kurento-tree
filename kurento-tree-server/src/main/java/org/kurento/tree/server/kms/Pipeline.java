package org.kurento.tree.server.kms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kurento.tree.server.app.TreeElementSession;

import com.google.common.collect.Iterables;

public class Pipeline extends KurentoObj {

	protected Kms kms;
	protected List<WebRtc> webRtcs = new ArrayList<>();
	protected List<Plumber> plumbers = new ArrayList<>();

	public Pipeline(Kms kms) {
		this.kms = kms;
	}

	public Kms getKms() {
		return kms;
	}

	public WebRtc createWebRtc(TreeElementSession session) {

		checkReleased();

		WebRtc webRtc = newWebRtc(session);
		webRtcs.add(webRtc);
		return webRtc;
	}

	public Plumber createPlumber() {

		checkReleased();

		Plumber plumber = newPlumber();
		plumbers.add(plumber);
		return plumber;
	}

	public List<WebRtc> getWebRtcs() {

		checkReleased();

		return webRtcs;
	}

	public List<Plumber> getPlumbers() {

		checkReleased();

		return plumbers;
	}

	public Iterable<Element> getElements() {

		checkReleased();

		return Iterables.concat(webRtcs, plumbers);
	}

	public Plumber[] link(Pipeline sinkPipeline) {

		checkReleased();

		Plumber sourcePipelinePlumber = this.createPlumber();
		Plumber sinkPipelinePlumber = sinkPipeline.createPlumber();

		sourcePipelinePlumber.link(sinkPipelinePlumber);

		return new Plumber[] { sourcePipelinePlumber, sinkPipelinePlumber };
	}

	protected WebRtc newWebRtc(TreeElementSession session) {

		checkReleased();

		return new WebRtc(this);
	}

	protected Plumber newPlumber() {

		checkReleased();

		return new Plumber(this);
	}

	void removeElement(Element element) {

		checkReleased();

		this.webRtcs.remove(element);
		this.plumbers.remove(element);
	}

	@Override
	public String toString() {
		return "[webRtcs=" + webRtcs.size() + ", plumbers=" + plumbers.size()
				+ "]";
	}

	@Override
	public void release() {

		Iterator<Element> it = getElements().iterator();
		while (it.hasNext()) {
			Element element = it.next();
			it.remove();
			element.release();
		}

		this.getKms().removePipeline(this);

		super.release();
	}

}
