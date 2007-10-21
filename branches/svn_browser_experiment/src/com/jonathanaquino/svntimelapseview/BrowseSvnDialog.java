/*
 * Copyright (c) 2005-2007 Flamingo Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of Flamingo Kirill Grouchnikov nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
package com.jonathanaquino.svntimelapseview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.jdesktop.swingworker.SwingWorker;
import org.jvnet.flamingo.bcb.BreadcrumbBar;
import org.jvnet.flamingo.bcb.BreadcrumbBarEvent;
import org.jvnet.flamingo.bcb.BreadcrumbBarExceptionHandler;
import org.jvnet.flamingo.bcb.BreadcrumbBarListener;
import org.jvnet.flamingo.bcb.BreadcrumbItem;
import org.jvnet.flamingo.bcb.BreadcrumbBarCallBack.KeyValuePair;
import org.jvnet.flamingo.bcb.core.BreadcrumbMultiSvnSelector;
import org.jvnet.flamingo.ide.MessageListDialog;

import com.jonathanaquino.svntimelapseview.helpers.MiscHelper;

public class BrowseSvnDialog extends JFrame {

	public class PathListRenderer extends JLabel implements ListCellRenderer {
		public PathListRenderer() {
			this.setBorder(new EmptyBorder(1, 1, 1, 1));
			this.setOpaque(true);
		}
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			String path = (String) value;
			this.setText(path);
			Color back = (index % 2 == 0) ? new Color(250, 250, 250) : new Color(240, 240, 240);
			if (isSelected) back = new Color(220, 220, 240);
			this.setBackground(back);
			return this;
		}
	}

	public static class SvnFolderListModel extends AbstractListModel {
		private ArrayList<String> entries = new ArrayList<String>();
		public void add(String entry) {
			entries.add(entry);
		}
		public void sort() {
			Collections.sort(entries);
		}
		public Object getElementAt(int index) {
			return entries.get(index);
		}
		public int getSize() {
			return entries.size();
		}
	}

	public BrowseSvnDialog() {
		final BreadcrumbMultiSvnSelector bar = new BreadcrumbMultiSvnSelector();
		bar.addSvnRepositoryInfo(new BreadcrumbMultiSvnSelector.SvnRepositoryInfo("SVNKit", "http://svn-time-lapse-view.googlecode.com/svn/", "anonymous", "anonymous"));
		bar.addSvnRepositoryInfo(new BreadcrumbMultiSvnSelector.SvnRepositoryInfo("KDE", "svn://anonsvn.kde.org/home/kde/trunk", "anonymous", "anonymous"));
		bar.addSvnRepositoryInfo(new BreadcrumbMultiSvnSelector.SvnRepositoryInfo("Apache", "http://svn.apache.org/repos/asf", "anonymous", "anonymous"));
		bar.setThrowsExceptions(true);
		bar.addExceptionHandler(new BreadcrumbBarExceptionHandler() {
			public void onException(Throwable t) {
				MiscHelper.handleThrowable(t);
			}
		});
		JToolBar toolbar = new JToolBar();
		toolbar.setLayout(new BorderLayout(3, 0));
		toolbar.setFloatable(false);
		toolbar.add(bar, BorderLayout.CENTER);
		this.setLayout(new BorderLayout());
		this.add(toolbar, BorderLayout.NORTH);
		final JList entriesList = new JList();
		entriesList.setCellRenderer(new PathListRenderer());
		JScrollPane fileListScrollPane = new JScrollPane(entriesList);
		this.add(fileListScrollPane, BorderLayout.CENTER);
		bar.addListener(new BreadcrumbBarListener<String>() {
			public void breadcrumbBarEvent(final BreadcrumbBarEvent<String> event) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						MiscHelper.handleThrowables(new Closure() {
							public void execute() throws Exception {
								if (event.getType() == BreadcrumbBarEvent.PATH_CHANGED) {
									final BreadcrumbItem<String>[] newPath = (BreadcrumbItem<String>[]) event.getNewValue();
									System.out.println("New path is ");
									for (BreadcrumbItem<String> item : newPath) {
										System.out.println("\t" + item.getValue());
									}
									if (newPath.length > 0) {
										SwingWorker<List<KeyValuePair<String>>, Void> worker = new SwingWorker<List<KeyValuePair<String>>, Void>() {
											@Override
											protected List<KeyValuePair<String>> doInBackground() {												
												return bar.getCallback().getLeafs(newPath);
											}

											@Override
											protected void done() {
												MiscHelper.handleThrowables(new Closure() {
													public void execute() throws Exception {
														SvnFolderListModel model = new SvnFolderListModel();
														List<KeyValuePair<String>> leafs = get();
														if (leafs != null) {
															for (KeyValuePair<String> leaf : leafs) {
																model.add(leaf.key);
															}
														}
														model.sort();
														entriesList.setModel(model);	
													}						
												});
											}
										};
										worker.execute();
									}
									return;
								}
								if (event.getType() == BreadcrumbBarEvent.MEMORY_CHANGED) {
									BreadcrumbBar<String> src = (BreadcrumbBar<String>) event.getSource();
									List<BreadcrumbItem<String>[]> memory = src.getMemory();
									DefaultListModel memoryModel = new DefaultListModel();
									for (BreadcrumbItem<String>[] memoryPath : memory) {
										memoryModel.addElement(memoryPath);
									}
								}								
							}						
						});
					}
				});
			}
		});
	}

	public static void main(String... args) {
		try {
			System.setProperty("java.net.useSystemProxies", "true");
		} catch (SecurityException e) {
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				BrowseSvnDialog test = new BrowseSvnDialog();
				test.setTitle("Browse SVN");
				test.setSize(700, 400);
				test.setLocation(300, 100);
				test.setVisible(true);
				test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		});
	}
}
